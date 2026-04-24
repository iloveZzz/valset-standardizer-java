package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.rule.ConditionRuleParser;
import com.yss.valset.transfer.domain.rule.JSONUtils;
import com.yss.valset.transfer.domain.rule.ScriptRuleEngineAdapter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * 邮件附件处理器，负责把单封邮件解析成识别上下文，并支持按 mailId 将附件重新落盘。
 */
@Component
public class EmailAttachmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(EmailAttachmentProcessor.class);
    private static final int ATTACHMENT_BUFFER_SIZE = 128 * 1024;

    private final ScriptRuleEngineAdapter scriptRuleEngineAdapter;

    public EmailAttachmentProcessor(ScriptRuleEngineAdapter scriptRuleEngineAdapter) {
        this.scriptRuleEngineAdapter = scriptRuleEngineAdapter;
    }

    /**
     * 将单封邮件转换成识别上下文集合。
     */
    AttachmentExtractionResult extract(TransferSource source, EmailSourceConfig config, Message message, String mailId, int emailSequence) throws Exception {
        MailMessageSnapshot mailSnapshot = buildMailMessageSnapshot(source, message);
        List<RecognitionContext> contexts = new ArrayList<>();
        int attachmentCount = 0;
        Object content = mailSnapshot.content();
        if (content instanceof Multipart multipart) {
            List<AttachmentPartEntry> attachmentEntries = collectAttachmentEntries(multipart);
            attachmentCount = attachmentEntries.size();
            List<String> attachmentNames = new ArrayList<>();
            for (AttachmentPartEntry attachmentEntry : attachmentEntries) {
                if (attachmentEntry == null || attachmentEntry.fileName() == null || attachmentEntry.fileName().isBlank()) {
                    continue;
                }
                RecognitionContext context = buildAttachmentContextIfAccepted(
                        config,
                        mailSnapshot,
                        mailId,
                        attachmentCount,
                        attachmentNames,
                        attachmentEntry
                );
                if (context != null) {
                    contexts.add(context);
                }
            }
        }
        log.info("邮件解析完成，第{}封，mailId={}，主题={}，附件数={}", emailSequence, mailId, mailSnapshot.subject(), contexts.size());
        return new AttachmentExtractionResult(contexts, attachmentCount, contexts.size());
    }

    /**
     * 根据 mailId 和附件元数据，将指定附件重新写入临时文件。
     */
    Path materialize(EmailSourceConfig config, com.yss.valset.transfer.domain.model.TransferObject transferObject) throws Exception {
        return resolveAndMaterializeAttachment(config, transferObject);
    }

    /**
     * 构建邮件会话参数，和连接器主流程保持一致。
     */
    Properties buildMailSessionProperties(EmailSourceConfig config) {
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
            properties.put("mail." + config.protocol() + ".partialfetch", "false");
        }
        applyTimeouts(properties, config.protocol(), config.timeoutMillis());
        return properties;
    }

    /**
     * 组装邮件快照，统一承载发件人、收件人、主题、正文和规则表达式。
     */
    private MailMessageSnapshot buildMailMessageSnapshot(TransferSource source, Message message) throws Exception {
        String sender = addressList(message.getFrom());
        String recipientsTo = addressList(message.getRecipients(Message.RecipientType.TO));
        String recipientsCc = addressList(message.getRecipients(Message.RecipientType.CC));
        String recipientsBcc = addressList(message.getRecipients(Message.RecipientType.BCC));
        String subject = safeSubject(message);
        Date sentDate = message.getSentDate();
        String mailConditionExpression = resolveMailConditionExpression(source);
        Object content = message.getContent();
        String body = extractBodyText(content);
        return new MailMessageSnapshot(sender, recipientsTo, recipientsCc, recipientsBcc, subject, sentDate, body, content, mailConditionExpression);
    }

    private RecognitionContext buildAttachmentContextIfAccepted(
            EmailSourceConfig config,
            MailMessageSnapshot mailSnapshot,
            String mailId,
            int attachmentCount,
            List<String> attachmentNames,
            AttachmentPartEntry attachmentEntry) throws Exception {
        BodyPart bodyPart = attachmentEntry.bodyPart();
        String fileName = attachmentEntry.fileName();
        attachmentNames.add(fileName);
        String contentType = bodyPart.getContentType();
        String mimeType = normalizeMimeType(contentType);
        Path tempFile = null;
        String contentFingerprint = nameAndSizeFingerprint(fileName, bodyPart.getSize());
        Long attachmentSize = bodyPart.getSize() < 0 ? null : (long) bodyPart.getSize();
        String attachmentFileType = resolveAttachmentFileType(fileName);
        Map<String, Object> attrs = buildAttachmentAttributes(
                config,
                mailSnapshot,
                mailId,
                attachmentCount,
                attachmentEntry,
                fileName,
                contentType,
                contentFingerprint,
                attachmentSize,
                attachmentFileType,
                attachmentNames
        );
        if (!acceptMailAttachment(mailSnapshot.mailConditionExpression(), attrs)) {
            log.info("邮件附件未命中收取条件，mailId={}，附件名={}，attachmentFileType={}，attachmentCount={}，limit={}，跳过接收",
                    mailId,
                    fileName,
                    attachmentFileType,
                    attachmentCount,
                    attachmentCount);
            return null;
        }
        try {
            AttachmentMaterializationResult materializedAttachment = writeAttachmentToTempFile(bodyPart, fileName);
            tempFile = materializedAttachment.tempFile();
            contentFingerprint = materializedAttachment.fingerprint();
            attachmentSize = materializedAttachment.size();
            attrs.put(TransferConfigKeys.CONTENT_FINGERPRINT, contentFingerprint);
            attrs.put(TransferConfigKeys.ATTACHMENT_SIZE, attachmentSize);
            attrs.put("fileSize", attachmentSize);
            log.info("邮件附件已落本地临时文件，mailId={}，附件名={}，tempPath={}，size={}", mailId, fileName, tempFile.toAbsolutePath(), attachmentSize);
        } catch (Exception materializeException) {
            attrs.put("attachmentMaterializeFailed", true);
            attrs.put("attachmentMaterializeError", materializeException.getMessage());
            log.warn("邮件附件临时落盘失败，继续按元数据处理，mailId={}，附件名={}，reason={}",
                    mailId,
                    fileName,
                    materializeException.getMessage(),
                    materializeException);
        }
        return new RecognitionContext(
                SourceType.EMAIL,
                config.sourceCode(),
                fileName,
                mimeType,
                attachmentSize,
                mailSnapshot.sender(),
                mailSnapshot.recipientsTo(),
                mailSnapshot.recipientsCc(),
                mailSnapshot.recipientsBcc(),
                mailSnapshot.subject(),
                mailSnapshot.body(),
                mailId,
                config.protocol(),
                config.folder(),
                tempFile == null ? null : tempFile.toAbsolutePath().toString(),
                attrs
        );
    }

    private Map<String, Object> buildAttachmentAttributes(
            EmailSourceConfig config,
            MailMessageSnapshot mailSnapshot,
            String mailId,
            int attachmentCount,
            AttachmentPartEntry attachmentEntry,
            String fileName,
            String contentType,
            String contentFingerprint,
            Long attachmentSize,
            String attachmentFileType,
            List<String> attachmentNames) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put(TransferConfigKeys.MAIL_ID, mailId);
        attrs.put("messageId", mailId);
        attrs.put(TransferConfigKeys.MAIL_FROM, mailSnapshot.sender());
        attrs.put(TransferConfigKeys.MAIL_TO, mailSnapshot.recipientsTo());
        attrs.put(TransferConfigKeys.MAIL_CC, mailSnapshot.recipientsCc());
        attrs.put(TransferConfigKeys.MAIL_BCC, mailSnapshot.recipientsBcc());
        attrs.put(TransferConfigKeys.MAIL_SUBJECT, mailSnapshot.subject());
        attrs.put(TransferConfigKeys.MAIL_BODY, mailSnapshot.body());
        attrs.put(TransferConfigKeys.MAIL_PROTOCOL, config.protocol());
        attrs.put(TransferConfigKeys.MAIL_FOLDER, config.folder());
        attrs.put(TransferConfigKeys.CHECKPOINT_KEY, mailId);
        attrs.put(TransferConfigKeys.CHECKPOINT_REF, mailId);
        attrs.put(TransferConfigKeys.CHECKPOINT_NAME, fileName);
        attrs.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, shortFingerprint(mailId + "|" + fileName + "|" + attachmentEntry.index()));
        attrs.put(TransferConfigKeys.CONTENT_FINGERPRINT, contentFingerprint);
        attrs.put(TransferConfigKeys.ATTACHMENT_INDEX, attachmentEntry.index());
        attrs.put(TransferConfigKeys.ATTACHMENT_NAME, fileName);
        attrs.put(TransferConfigKeys.ATTACHMENT_CONTENT_TYPE, contentType);
        attrs.put(TransferConfigKeys.ATTACHMENT_SIZE, attachmentSize);
        attrs.put(TransferConfigKeys.ATTACHMENT_COUNT, attachmentCount);
        attrs.put(TransferConfigKeys.FILE_TYPE, attachmentFileType);
        attrs.put("attachmentFileType", attachmentFileType);
        attrs.put("fileType", attachmentFileType);
        attrs.put("fileSize", attachmentSize);
        attrs.put("limit", attachmentCount);
        attrs.put(TransferConfigKeys.ATTACHMENT_NAMES, new ArrayList<>(attachmentNames));
        attrs.put("sentDate", mailSnapshot.sentDate() == null ? null : Instant.ofEpochMilli(mailSnapshot.sentDate().getTime()).toString());
        return attrs;
    }

    private String resolveMailConditionExpression(TransferSource source) {
        if (source == null || source.connectionConfig() == null) {
            return null;
        }
        Object rawMailCondition = source.connectionConfig().get("mailCondition");
        if (rawMailCondition == null) {
            return null;
        }
        String json = rawMailCondition instanceof String ? String.valueOf(rawMailCondition) : JSONUtils.toJsonString(rawMailCondition);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return ConditionRuleParser.fromJsonString(json).toConditionRule();
        } catch (Exception e) {
            throw new IllegalStateException("邮箱条件解析失败，sourceCode=" + source.sourceCode() + ", mailCondition=" + json, e);
        }
    }

    private boolean acceptMailAttachment(String mailConditionExpression, Map<String, Object> variables) {
        if (mailConditionExpression == null || mailConditionExpression.isBlank()) {
            return true;
        }
        return scriptRuleEngineAdapter.evaluateBooleanExpression(mailConditionExpression, variables);
    }

    /**
     * 定位目标附件并完成临时落盘。
     */
    private Path resolveAndMaterializeAttachment(EmailSourceConfig config, com.yss.valset.transfer.domain.model.TransferObject transferObject) throws Exception {
        Properties properties = buildMailSessionProperties(config);
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
                Map<String, Object> fileMeta = transferObject.fileMeta() == null ? Map.of() : transferObject.fileMeta();
                int attachmentIndex = resolveAttachmentIndex(fileMeta);
                String attachmentName = resolveAttachmentName(fileMeta, transferObject.originalName());
                ResolvedAttachment resolvedAttachment = resolveTargetAttachment(message, transferObject.mailId(), attachmentIndex, attachmentName);
                return writeAttachmentToTempFile(resolvedAttachment.bodyPart(), resolvedAttachment.attachmentName()).tempFile().toAbsolutePath();
            }
        }
    }

    /**
     * 先读取邮件内容，再从多部分内容里定位目标附件。
     */
    private ResolvedAttachment resolveTargetAttachment(Message message, String mailId, int attachmentIndex, String attachmentName) throws Exception {
        Object content = message.getContent();
        if (!(content instanceof Multipart multipart)) {
            throw new IllegalStateException("邮件不包含附件，mailId=" + mailId);
        }
        BodyPart bodyPart = resolveAttachmentPart(multipart, attachmentIndex, attachmentName);
        if (bodyPart == null) {
            throw new IllegalStateException("未找到邮件附件，mailId=" + mailId + ", attachmentName=" + attachmentName + ", attachmentIndex=" + attachmentIndex);
        }
        String resolvedAttachmentName = firstNonBlank(
                normalizeAttachmentName(bodyPart.getFileName()),
                attachmentName);
        return new ResolvedAttachment(bodyPart, resolvedAttachmentName);
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
            if (!isAttachmentCandidate(bodyPart, normalizeAttachmentName(bodyPart.getFileName()))) {
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

    private List<AttachmentPartEntry> collectAttachmentEntries(Multipart multipart) throws Exception {
        List<AttachmentPartEntry> attachmentEntries = new ArrayList<>();
        collectAttachmentEntries(multipart, attachmentEntries);
        return attachmentEntries;
    }

    private void collectAttachmentEntries(Multipart multipart, List<AttachmentPartEntry> attachmentEntries) throws Exception {
        if (multipart == null || attachmentEntries == null) {
            return;
        }
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart == null) {
                continue;
            }
            String fileName = normalizeAttachmentName(bodyPart.getFileName());
            if (isAttachmentCandidate(bodyPart, fileName)) {
                attachmentEntries.add(new AttachmentPartEntry(attachmentEntries.size(), bodyPart, fileName));
                continue;
            }
            Object childContent = null;
            try {
                childContent = bodyPart.getContent();
            } catch (Exception ignored) {
                // 不能读取内容时，不阻断其它附件继续扫描。
            }
            if (childContent instanceof Multipart nestedMultipart) {
                collectAttachmentEntries(nestedMultipart, attachmentEntries);
            } else if (childContent instanceof Message nestedMessage) {
                Object nestedMessageContent = nestedMessage.getContent();
                if (nestedMessageContent instanceof Multipart nestedMessageMultipart) {
                    collectAttachmentEntries(nestedMessageMultipart, attachmentEntries);
                }
            }
        }
    }

    private boolean isAttachmentCandidate(BodyPart bodyPart, String fileName) throws Exception {
        if (bodyPart == null) {
            return false;
        }
        if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
            return true;
        }
        if (fileName != null && !fileName.isBlank()) {
            return true;
        }
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

    private String resolveAttachmentFileType(String fileName) {
        String normalized = normalizeAttachmentName(fileName);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        int lastDot = lower.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= lower.length() - 1) {
            return lower;
        }
        return lower.substring(lastDot + 1);
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
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception ignored) {
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

    private void applyTimeouts(Properties properties, String protocol, int timeoutMillis) {
        if (properties == null || protocol == null || protocol.isBlank() || timeoutMillis <= 0) {
            return;
        }
        properties.put("mail." + protocol + ".connectiontimeout", String.valueOf(timeoutMillis));
        properties.put("mail." + protocol + ".timeout", String.valueOf(timeoutMillis));
        properties.put("mail." + protocol + ".writetimeout", String.valueOf(timeoutMillis));
    }

    private record MailMessageSnapshot(
            String sender,
            String recipientsTo,
            String recipientsCc,
            String recipientsBcc,
            String subject,
            Date sentDate,
            String body,
            Object content,
            String mailConditionExpression) {
    }

    record AttachmentExtractionResult(List<RecognitionContext> contexts, int attachmentCount, int acceptedCount) {
    }

    private record AttachmentPartEntry(int index, BodyPart bodyPart, String fileName) {
    }

    private record ResolvedAttachment(BodyPart bodyPart, String attachmentName) {
    }

    private record AttachmentMaterializationResult(Path tempFile, String fingerprint, long size) {
    }
}
