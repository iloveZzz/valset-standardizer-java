package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeBodyPart;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * 邮件来源连接器，支持通过 IMAP/IMAPS/POP3/POP3S 收取邮件附件，并保留邮件正文和头信息。
 */
@Component
public class EmailSourceConnector implements SourceConnector {

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
        EmailConfig config = EmailConfig.from(source);
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

        List<RecognitionContext> result = new ArrayList<>();
        Session session = Session.getInstance(properties);
        try (Store store = session.getStore(config.protocol())) {
            store.connect(config.host(), config.port(), config.username(), config.password());
            try (Folder folder = store.getFolder(config.folder())) {
                folder.open(Folder.READ_ONLY);
                Message[] messages = folder.getMessages();
                int limit = config.limit() > 0 ? Math.min(config.limit(), messages.length) : messages.length;
                for (int i = Math.max(0, messages.length - limit); i < messages.length; i++) {
                    Message message = messages[i];
                    result.addAll(extractAttachments(config, message));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("收取邮件附件失败，folder=" + config.folder() + ", protocol=" + config.protocol(), e);
        }
        return result;
    }

    private List<RecognitionContext> extractAttachments(EmailConfig config, Message message) throws Exception {
        List<RecognitionContext> contexts = new ArrayList<>();
        String sender = addressList(message.getFrom());
        String recipientsTo = addressList(message.getRecipients(Message.RecipientType.TO));
        String recipientsCc = addressList(message.getRecipients(Message.RecipientType.CC));
        String recipientsBcc = addressList(message.getRecipients(Message.RecipientType.BCC));
        String subject = message.getSubject();
        String body = extractBodyText(message);
        Date sentDate = message.getSentDate();
        String mailId = resolveMailId(message, config);

        Object content = message.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && !looksLikeAttachment(bodyPart)) {
                    continue;
                }
                String fileName = bodyPart.getFileName();
                if (fileName == null || fileName.isBlank()) {
                    continue;
                }
                Path tempFile = Files.createTempFile("transfer-email-", "-" + sanitizeFileName(fileName));
                try (InputStream inputStream = bodyPart.getInputStream()) {
                    Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Map<String, Object> attrs = new LinkedHashMap<>();
                attrs.put("mailId", mailId);
                attrs.put("messageId", mailId);
                attrs.put("mailFrom", sender);
                attrs.put("mailTo", recipientsTo);
                attrs.put("mailCc", recipientsCc);
                attrs.put("mailBcc", recipientsBcc);
                attrs.put("mailSubject", subject);
                attrs.put("mailBody", body);
                attrs.put("mailProtocol", config.protocol());
                attrs.put("mailFolder", config.folder());
                attrs.put("sentDate", sentDate == null ? null : Instant.ofEpochMilli(sentDate.getTime()).toString());
                attrs.put("attachmentIndex", i);
                attrs.put("attachmentName", fileName);
                attrs.put("tempPath", tempFile.toAbsolutePath().toString());
                contexts.add(new RecognitionContext(
                        SourceType.EMAIL,
                        config.sourceCode(),
                        fileName,
                        bodyPart.getContentType(),
                        Files.size(tempFile),
                        sender,
                        recipientsTo,
                        recipientsCc,
                        recipientsBcc,
                        subject,
                        body,
                        mailId,
                        config.protocol(),
                        config.folder(),
                        tempFile.toAbsolutePath().toString(),
                        attrs
                ));
            }
        }
        return contexts;
    }

    private boolean looksLikeAttachment(BodyPart bodyPart) throws Exception {
        return bodyPart instanceof MimeBodyPart && bodyPart.getFileName() != null;
    }

    private String extractBodyText(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String stringContent) {
            return stringContent;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    continue;
                }
                builder.append(extractBodyText(bodyPart));
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
            }
            return builder.toString().trim();
        }
        return content == null ? null : String.valueOf(content);
    }

    private String addressList(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",");
        for (Address address : addresses) {
            if (address != null) {
                joiner.add(address.toString());
            }
        }
        String value = joiner.toString();
        return value.isBlank() ? null : value;
    }

    private String resolveMailId(Message message, EmailConfig config) throws Exception {
        String[] header = message.getHeader("Message-ID");
        if (header != null && header.length > 0 && header[0] != null && !header[0].isBlank()) {
            return header[0];
        }
        return config.protocol() + ":" + config.folder() + ":" + message.getMessageNumber();
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record EmailConfig(
            String protocol,
            String host,
            int port,
            String username,
            String password,
            String folder,
            boolean ssl,
            boolean startTls,
            int limit,
            String sourceCode
    ) {
        static EmailConfig from(TransferSource source) {
            Map<String, Object> config = source.connectionConfig() == null ? Map.of() : source.connectionConfig();
            String protocol = normalizeProtocol(stringValue(config, "protocol", "imap"));
            String host = requiredString(config, "host");
            int port = intValue(config, "port", defaultPort(protocol));
            String username = requiredString(config, "username");
            String password = requiredString(config, "password");
            String folder = stringValue(config, "folder", "INBOX");
            boolean ssl = booleanValue(config, "ssl", protocol.endsWith("s"));
            boolean startTls = booleanValue(config, "startTls", false);
            int limit = intValue(config, "limit", 0);
            String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank() ? username : source.sourceCode();
            return new EmailConfig(protocol, host, port, username, password, folder, ssl, startTls, limit, sourceCode);
        }

        private static String normalizeProtocol(String protocol) {
            if (protocol == null || protocol.isBlank()) {
                return "imap";
            }
            String value = protocol.trim().toLowerCase();
            if ("imap".equals(value) || "imaps".equals(value) || "pop3".equals(value) || "pop3s".equals(value)) {
                return value;
            }
            throw new IllegalArgumentException("不支持的邮件协议: " + protocol);
        }

        private static int defaultPort(String protocol) {
            return switch (protocol) {
                case "imaps" -> 993;
                case "pop3s" -> 995;
                case "pop3" -> 110;
                default -> 143;
            };
        }

        private static String requiredString(Map<String, Object> config, String key) {
            Object raw = config.get(key);
            if (raw == null || String.valueOf(raw).isBlank()) {
                throw new IllegalArgumentException("邮件来源缺少必要配置: " + key);
            }
            return String.valueOf(raw);
        }

        private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
            Object raw = config.get(key);
            return raw == null ? defaultValue : String.valueOf(raw);
        }

        private static int intValue(Map<String, Object> config, String key, int defaultValue) {
            Object raw = config.get(key);
            if (raw == null) {
                return defaultValue;
            }
            return Integer.parseInt(String.valueOf(raw));
        }

        private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
            Object raw = config.get(key);
            if (raw == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(String.valueOf(raw));
        }
    }
}
