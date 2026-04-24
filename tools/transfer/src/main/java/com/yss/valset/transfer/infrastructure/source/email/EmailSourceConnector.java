package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

/**
 * 邮件来源连接器，支持通过 IMAP/IMAPS/POP3/POP3S 收取邮件附件，并保留邮件正文和头信息。
 */
@Component
@RequiredArgsConstructor
public class EmailSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(EmailSourceConnector.class);
    private static final int ATTACHMENT_BUFFER_SIZE = 128 * 1024;
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;

    @Override
    public String type() {
        return SourceType.EMAIL.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.EMAIL;
    }

    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        EmailSourceConfig config = EmailSourceConfig.from(source);
        Properties properties = new Properties();
        properties.put("mail.store.protocol", config.protocol());
        properties.put("mail." + config.protocol() + ".host", config.host());
        properties.put("mail." + config.protocol() + ".port", String.valueOf(config.port()));
        properties.put("mail." + config.protocol() + ".auth", "true");
        if (config.ssl()) {
            properties.put("mail." + config.protocol() + ".ssl.enable", "true");
        }
        if (config.startTls()) {
            properties.put("mail." + config.protocol() + ".starttls.enable", "true");
        }
        if (config.protocol().startsWith("imap")) {
            // 关闭 IMAP 分片拉取，避免附件流长时间悬挂在服务端连接上。
            properties.put("mail." + config.protocol() + ".partialfetch", "false");
        }
        applyTimeouts(properties, config.protocol(), config.timeoutMillis());

        List<RecognitionContext> result = new ArrayList<>();
        Session session = Session.getInstance(properties);
        try (Store store = session.getStore(config.protocol())) {
            store.connect(config.host(), config.port(), config.username(), config.password());
            try (Folder folder = store.getFolder(config.folder())) {
                folder.open(Folder.READ_ONLY);
                UIDFolder uidFolder = folder instanceof UIDFolder ? (UIDFolder) folder : null;
                String cursor = readCursor(source);
                boolean seenCursor = cursor == null || cursor.isBlank();
                Message[] messages = folder.getMessages();
                SourceFetchLogSupport.logStart(log, "邮件", source, "folder", config.folder(), "邮件总数", messages.length);
                Set<String> processedInThisRun = new java.util.LinkedHashSet<>();
                int limit = config.effectiveLimit();
                int emailSequence = 0;
                for (Message message : messages) {
                    if (processedInThisRun.size() >= limit) {
                        break;
                    }
                    if (shouldStop(source)) {
                        log.info("邮件收取收到停止请求，提前结束，sourceId={}，sourceCode={}", source.sourceId(), source.sourceCode());
                        break;
                    }
                    String mailId = resolveMailId(message, config, uidFolder);
                    if (!seenCursor) {
                        if (cursor.equals(mailId)) {
                            seenCursor = true;
                        }
                        continue;
                    }

                    String subject = safeSubject(message);
                    if (source.sourceId() != null && transferSourceCheckpointGateway.existsProcessedItem(source.sourceId(), mailId)) {
                        recordScanCursor(source, config, mailId, subject, emailSequence + 1, "已处理");
                        continue;
                    }
                    emailSequence++;
                    log.info("收取邮件第{}封，mailId={}，主题={}", emailSequence, mailId, subject);
                    processedInThisRun.add(mailId);
                    List<RecognitionContext> contexts = extractAttachmentMetadata(source, config, message, mailId, emailSequence);
                    result.addAll(contexts);
                    if (contexts.isEmpty()) {
                        recordScanCursor(source, config, mailId, subject, emailSequence, "无附件");
                    }
                    log.info("邮件收取完成，第{}封，mailId={}，主题={}，附件数={}", emailSequence, mailId, subject, contexts.size());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("收取邮件附件失败，folder=" + config.folder() + ", protocol=" + config.protocol(), e);
        }
        return result;
    }

    @Override
    public Path materialize(TransferSource source, com.yss.valset.transfer.domain.model.TransferObject transferObject) {
        EmailSourceConfig config = EmailSourceConfig.from(source);
        if (transferObject == null || transferObject.mailId() == null || transferObject.mailId().isBlank()) {
            throw new IllegalStateException("邮件附件落盘失败，缺少 mailId");
        }
        try {
            return doMaterialize(config, transferObject);
        } catch (Exception e) {
            throw new IllegalStateException("邮件附件落盘失败，mailId=" + transferObject.mailId() + ", fileName=" + transferObject.originalName(), e);
        }
    }

    private List<RecognitionContext> extractAttachmentMetadata(TransferSource source, EmailSourceConfig config, Message message, String mailId, int emailSequence) throws Exception {
        List<RecognitionContext> contexts = new ArrayList<>();
        String sender = addressList(message.getFrom());
        String recipientsTo = addressList(message.getRecipients(Message.RecipientType.TO));
        String recipientsCc = addressList(message.getRecipients(Message.RecipientType.CC));
        String recipientsBcc = addressList(message.getRecipients(Message.RecipientType.BCC));
        String subject = message.getSubject();
        Date sentDate = message.getSentDate();

        Object content = message.getContent();
        String body = extractBodyText(content);
        if (content instanceof Multipart multipart) {
            List<String> attachmentNames = new ArrayList<>();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && !looksLikeAttachment(bodyPart)) {
                    continue;
                }
                if (shouldStop(source)) {
                    break;
                }
                String fileName = normalizeAttachmentName(bodyPart.getFileName());
                if (fileName == null || fileName.isBlank()) {
                    continue;
                }
                attachmentNames.add(fileName);
                String contentType = bodyPart.getContentType();
                String mimeType = normalizeMimeType(contentType);
                Path tempFile = null;
                String contentFingerprint = nameAndSizeFingerprint(fileName, bodyPart.getSize());
                Long attachmentSize = bodyPart.getSize() < 0 ? null : (long) bodyPart.getSize();
                Map<String, Object> attrs = new LinkedHashMap<>();
                attrs.put(TransferConfigKeys.MAIL_ID, mailId);
                attrs.put("messageId", mailId);
                attrs.put(TransferConfigKeys.MAIL_FROM, sender);
                attrs.put(TransferConfigKeys.MAIL_TO, recipientsTo);
                attrs.put(TransferConfigKeys.MAIL_CC, recipientsCc);
                attrs.put(TransferConfigKeys.MAIL_BCC, recipientsBcc);
                attrs.put(TransferConfigKeys.MAIL_SUBJECT, subject);
                attrs.put(TransferConfigKeys.MAIL_BODY, body);
                attrs.put(TransferConfigKeys.MAIL_PROTOCOL, config.protocol());
                attrs.put(TransferConfigKeys.MAIL_FOLDER, config.folder());
                attrs.put(TransferConfigKeys.CHECKPOINT_KEY, mailId);
                attrs.put(TransferConfigKeys.CHECKPOINT_REF, mailId);
                attrs.put(TransferConfigKeys.CHECKPOINT_NAME, fileName);
                attrs.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, shortFingerprint(mailId + "|" + fileName + "|" + i));
                attrs.put(TransferConfigKeys.CONTENT_FINGERPRINT, contentFingerprint);
                attrs.put(TransferConfigKeys.ATTACHMENT_INDEX, i);
                attrs.put(TransferConfigKeys.ATTACHMENT_NAME, fileName);
                attrs.put(TransferConfigKeys.ATTACHMENT_CONTENT_TYPE, contentType);
                attrs.put(TransferConfigKeys.ATTACHMENT_SIZE, bodyPart.getSize());
                attrs.put(TransferConfigKeys.ATTACHMENT_COUNT, attachmentNames.size());
                attrs.put(TransferConfigKeys.ATTACHMENT_NAMES, new ArrayList<>(attachmentNames));
                attrs.put("sentDate", sentDate == null ? null : Instant.ofEpochMilli(sentDate.getTime()).toString());
                try {
                    AttachmentMaterializationResult materializedAttachment = writeAttachmentToTempFile(bodyPart, fileName);
                    tempFile = materializedAttachment.tempFile();
                    contentFingerprint = materializedAttachment.fingerprint();
                    attachmentSize = materializedAttachment.size();
                    attrs.put(TransferConfigKeys.CONTENT_FINGERPRINT, contentFingerprint);
                    attrs.put(TransferConfigKeys.ATTACHMENT_SIZE, attachmentSize);
                    log.info("邮件附件已落本地临时文件，mailId={}，附件名={}，tempPath={}，size={}", mailId, fileName, tempFile.toAbsolutePath(), attachmentSize);
                } catch (Exception materializeException) {
                    log.warn("邮件附件临时落盘失败，继续按元数据处理，mailId={}，附件名={}，reason={}",
                            mailId,
                            fileName,
                            materializeException.getMessage(),
                            materializeException);
                }
                contexts.add(new RecognitionContext(
                        SourceType.EMAIL,
                        config.sourceCode(),
                        fileName,
                        mimeType,
                        attachmentSize,
                        sender,
                        recipientsTo,
                        recipientsCc,
                        recipientsBcc,
                        subject,
                        body,
                        mailId,
                        config.protocol(),
                        config.folder(),
                        tempFile == null ? null : tempFile.toAbsolutePath().toString(),
                        attrs
                ));
            }
        }
        log.info("邮件解析完成，第{}封，mailId={}，主题={}，附件数={}", emailSequence, mailId, subject, contexts.size());
        return contexts;
    }

    private String readCursor(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return null;
        }
        return transferSourceCheckpointGateway.findCheckpoint(source.sourceId(), TransferConfigKeys.CHECKPOINT_SCAN_CURSOR)
                .map(checkpoint -> checkpoint.checkpointValue())
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }

    private void recordScanCursor(TransferSource source, EmailSourceConfig config, String mailId, String subject, int emailSequence, String reason) {
        if (source == null || source.sourceId() == null || mailId == null || mailId.isBlank()) {
            return;
        }
        Map<String, Object> checkpointMeta = new LinkedHashMap<>();
        checkpointMeta.put(TransferConfigKeys.SOURCE_TYPE, source.sourceType() == null ? null : source.sourceType().name());
        checkpointMeta.put(TransferConfigKeys.SOURCE_CODE, source.sourceCode());
        checkpointMeta.put(TransferConfigKeys.TRIGGER_TYPE, "FETCH");
        checkpointMeta.put(TransferConfigKeys.MAIL_ID, mailId);
        checkpointMeta.put(TransferConfigKeys.MAIL_SUBJECT, subject);
        checkpointMeta.put(TransferConfigKeys.MAIL_FOLDER, config.folder());
        checkpointMeta.put(TransferConfigKeys.MAIL_PROTOCOL, config.protocol());
        checkpointMeta.put("emailSequence", emailSequence);
        checkpointMeta.put("scanReason", reason);
        checkpointMeta.put("processedAt", Instant.now().toString());
        transferSourceCheckpointGateway.saveCheckpoint(new TransferSourceCheckpoint(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                TransferConfigKeys.CHECKPOINT_SCAN_CURSOR,
                mailId,
                checkpointMeta,
                Instant.now(),
                Instant.now()
        ));
    }

    private Path doMaterialize(EmailSourceConfig config, com.yss.valset.transfer.domain.model.TransferObject transferObject) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", config.protocol());
        properties.put("mail." + config.protocol() + ".host", config.host());
        properties.put("mail." + config.protocol() + ".port", String.valueOf(config.port()));
        properties.put("mail." + config.protocol() + ".auth", "true");
        if (config.ssl()) {
            properties.put("mail." + config.protocol() + ".ssl.enable", "true");
        }
        if (config.startTls()) {
            properties.put("mail." + config.protocol() + ".starttls.enable", "true");
        }
        if (config.protocol().startsWith("imap")) {
            // 关闭 IMAP 分片拉取，避免附件流长时间悬挂在服务端连接上。
            properties.put("mail." + config.protocol() + ".partialfetch", "false");
        }
        applyTimeouts(properties, config.protocol(), config.timeoutMillis());
        Session session = Session.getInstance(properties);
        try (Store store = session.getStore(config.protocol())) {
            store.connect(config.host(), config.port(), config.username(), config.password());
            try (Folder folder = store.getFolder(config.folder())) {
                folder.open(Folder.READ_ONLY);
                UIDFolder uidFolder = folder instanceof UIDFolder ? (UIDFolder) folder : null;
                Message message = resolveMessage(folder, uidFolder, transferObject.mailId(), config);
                if (message == null) {
                    throw new IllegalStateException("未找到对应邮件，mailId=" + transferObject.mailId());
                }
                Object content = message.getContent();
                if (!(content instanceof Multipart multipart)) {
                    throw new IllegalStateException("邮件不包含附件，mailId=" + transferObject.mailId());
                }
                Map<String, Object> fileMeta = transferObject.fileMeta() == null ? Map.of() : transferObject.fileMeta();
                int attachmentIndex = resolveAttachmentIndex(fileMeta);
                String attachmentName = resolveAttachmentName(fileMeta, transferObject.originalName());
                BodyPart bodyPart = resolveAttachmentPart(multipart, attachmentIndex, attachmentName);
                if (bodyPart == null) {
                    throw new IllegalStateException("未找到邮件附件，mailId=" + transferObject.mailId() + ", attachmentName=" + attachmentName + ", attachmentIndex=" + attachmentIndex);
                }
                return writeAttachmentToTempFile(bodyPart, attachmentName).tempFile().toAbsolutePath();
            }
        }
    }

    private Message resolveMessage(Folder folder, UIDFolder uidFolder, String mailId, EmailSourceConfig config) throws Exception {
        if (mailId == null || mailId.isBlank()) {
            return null;
        }
        if (uidFolder != null) {
            String prefix = config.protocol() + ":" + config.folder() + ":";
            if (mailId.startsWith(prefix)) {
                String tail = mailId.substring(prefix.length());
                try {
                    long uid = Long.parseLong(tail);
                    Message message = uidFolder.getMessageByUID(uid);
                    if (message != null) {
                        return message;
                    }
                } catch (NumberFormatException ignored) {
                    // 继续使用消息头匹配。
                }
            }
        }
        Message[] messages = folder.getMessages();
        for (Message message : messages) {
            if (mailId.equals(resolveMailId(message, config, uidFolder))) {
                return message;
            }
        }
        return null;
    }

    private int resolveAttachmentIndex(Map<String, Object> fileMeta) {
        Object raw = fileMeta == null ? null : fileMeta.get(TransferConfigKeys.ATTACHMENT_INDEX);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String resolveAttachmentName(Map<String, Object> fileMeta, String fallback) {
        Object raw = fileMeta == null ? null : fileMeta.get(TransferConfigKeys.ATTACHMENT_NAME);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return normalizeAttachmentName(fallback);
        }
        return normalizeAttachmentName(String.valueOf(raw));
    }

    private String normalizeMimeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String normalized = contentType.trim();
        int semicolon = normalized.indexOf(';');
        if (semicolon > 0) {
            normalized = normalized.substring(0, semicolon).trim();
        }
        return normalized.isBlank() ? null : normalized;
    }

    private BodyPart resolveAttachmentPart(Multipart multipart, int attachmentIndex, String attachmentName) throws Exception {
        int currentIndex = -1;
        BodyPart fallback = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && !looksLikeAttachment(bodyPart)) {
                continue;
            }
            currentIndex++;
            if (attachmentIndex >= 0 && currentIndex == attachmentIndex) {
                return bodyPart;
            }
            String fileName = normalizeAttachmentName(bodyPart.getFileName());
            if (attachmentName != null && !attachmentName.isBlank() && attachmentName.equals(fileName)) {
                return bodyPart;
            }
            if (fallback == null) {
                fallback = bodyPart;
            }
        }
        return fallback;
    }

    private boolean looksLikeAttachment(BodyPart bodyPart) throws Exception {
        return bodyPart instanceof MimeBodyPart && normalizeAttachmentName(bodyPart.getFileName()) != null;
    }

    private String extractBodyText(Object content) throws Exception {
        if (content instanceof String stringContent) {
            return limitBody(stringContent);
        }
        if (content instanceof Multipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    continue;
                }
                Object childContent = bodyPart.getContent();
                builder.append(extractBodyText(childContent));
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
            }
            return limitBody(builder.toString().trim());
        }
        return content == null ? null : limitBody(String.valueOf(content));
    }

    private String addressList(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",");
        for (Address address : addresses) {
            if (address != null) {
                joiner.add(formatAddress(address));
            }
        }
        String value = joiner.toString();
        return value.isBlank() ? null : value;
    }

    private String formatAddress(Address address) {
        if (address instanceof InternetAddress internetAddress) {
            String email = normalizeAddressPart(internetAddress.getAddress());
            String personal = decodePersonal(internetAddress.getPersonal());
            if (personal != null && !personal.isBlank()) {
                return personal + " <" + email + ">";
            }
            return email;
        }
        return decodePersonal(address.toString());
    }

    private String decodePersonal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MimeUtility.decodeText(value);
        } catch (Exception ignored) {
            return value;
        }
    }

    private String normalizeAddressPart(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String resolveMailId(Message message, EmailSourceConfig config, UIDFolder uidFolder) throws Exception {
        String[] header = message.getHeader("Message-ID");
        if (header != null && header.length > 0 && header[0] != null && !header[0].isBlank()) {
            return header[0];
        }
        if (uidFolder != null) {
            long uid = uidFolder.getUID(message);
            if (uid > 0) {
                return config.protocol() + ":" + config.folder() + ":" + uid;
            }
        }
        return config.protocol() + ":" + config.folder() + ":" + message.getMessageNumber();
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String normalizeAttachmentName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return rawName;
        }
        String decoded = rawName;
        try {
            decoded = MimeUtility.decodeText(rawName);
        } catch (Exception ignored) {
            // 保持原样兜底。
        }
        decoded = decoded.replace('\u00A0', ' ');
        decoded = decoded.replaceAll("[\\r\\n\\t]+", " ");
        decoded = decoded.replaceAll("\\s+", " ").trim();
        decoded = decoded.replaceAll("\\s*([._-])\\s*", "$1");
        return decoded;
    }

    private String buildAttachmentTempSuffix(String fileName) {
        String sanitized = sanitizeFileName(fileName);
        String extension = "";
        int lastDot = sanitized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < sanitized.length() - 1) {
            extension = sanitized.substring(lastDot);
        }
        return "-" + shortFingerprint(sanitized) + extension;
    }

    private String limitBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        int maxLength = 4096;
        if (body.length() <= maxLength) {
            return body;
        }
        return body.substring(0, maxLength);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String safeSubject(Message message) {
        if (message == null) {
            return null;
        }
        try {
            return message.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private String shortFingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(hashed.length, 6); i++) {
                builder.append(String.format("%02x", hashed[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value == null ? 0 : value.hashCode());
        }
    }

    private AttachmentMaterializationResult writeAttachmentToTempFile(BodyPart bodyPart, String attachmentName) {
        if (bodyPart == null) {
            throw new IllegalStateException("邮件附件为空，无法转存");
        }
        Path tempFile = null;
        try {
            String tempSuffix = buildAttachmentTempSuffix(firstNonBlank(
                    normalizeAttachmentName(bodyPart.getFileName()),
                    attachmentName));
            tempFile = Files.createTempFile("transfer-email-", tempSuffix);
            log.info("邮件附件准备转存，attachmentName={}，tempFile={}", attachmentName, tempFile);
            long size = 0L;
            InputStream inputStream;
            try {
                inputStream = bodyPart.getInputStream();
            } catch (Exception openException) {
                throw new IllegalStateException("邮件附件打开输入流失败，attachmentName=" + attachmentName, openException);
            }
            try (InputStream openedInputStream = inputStream;
                 java.io.OutputStream outputStream = Files.newOutputStream(tempFile)) {
                log.info("邮件附件开始写入临时文件，attachmentName={}，tempFile={}", attachmentName, tempFile);
                byte[] buffer = new byte[ATTACHMENT_BUFFER_SIZE];
                int read;
                while ((read = openedInputStream.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    outputStream.write(buffer, 0, read);
                    size += read;
                }
            }
            log.info("邮件附件写入临时文件完成，attachmentName={}，tempFile={}，size={}", attachmentName, tempFile, size);
            return new AttachmentMaterializationResult(tempFile, nameAndSizeFingerprint(attachmentName, size), size);
        } catch (Exception e) {
            try {
                // 如果文件只创建了一半，尽量清掉残留临时文件。
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception ignored) {
                // 失败时不再遮蔽原始异常。
            }
            throw new IllegalStateException("邮件附件落盘失败，attachmentName=" + attachmentName, e);
        }
    }

    private String nameAndSizeFingerprint(String name, long size) {
        String raw = firstNonBlank(name, "attachment") + ":" + size;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private boolean shouldStop(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return false;
        }
        return transferSourceGateway.findById(source.sourceId())
                .map(current -> "STOPPING".equalsIgnoreCase(current.ingestStatus())
                        || "STOPPED".equalsIgnoreCase(current.ingestStatus()))
                .orElse(false);
    }

    private void applyTimeouts(Properties properties, String protocol, int timeoutMillis) {
        if (properties == null || protocol == null || protocol.isBlank() || timeoutMillis <= 0) {
            return;
        }
        properties.put("mail." + protocol + ".connectiontimeout", String.valueOf(timeoutMillis));
        properties.put("mail." + protocol + ".timeout", String.valueOf(timeoutMillis));
        properties.put("mail." + protocol + ".writetimeout", String.valueOf(timeoutMillis));
    }

    private record AttachmentMaterializationResult(Path tempFile, String fingerprint, long size) {
    }
}
