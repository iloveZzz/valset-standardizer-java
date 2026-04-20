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

import java.io.IOException;
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
import java.util.Objects;
import java.util.Properties;

/**
 * 邮件来源连接器，支持通过 IMAP/IMAPS 收取附件。
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
        if (config.ssl()) {
            properties.put("mail." + config.protocol() + ".ssl.enable", "true");
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
            throw new IllegalStateException("收取邮件附件失败，folder=" + config.folder(), e);
        }
        return result;
    }

    private List<RecognitionContext> extractAttachments(EmailConfig config, Message message) throws Exception {
        List<RecognitionContext> contexts = new ArrayList<>();
        String sender = senderOf(message);
        String subject = message.getSubject();
        Date sentDate = message.getSentDate();
        String messageId = message.getHeader("Message-ID") == null ? null : String.join(",", message.getHeader("Message-ID"));

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
                attrs.put("messageId", messageId);
                attrs.put("sentDate", sentDate == null ? null : Instant.ofEpochMilli(sentDate.getTime()).toString());
                attrs.put("folder", config.folder());
                attrs.put("attachmentIndex", i);
                attrs.put("tempPath", tempFile.toAbsolutePath().toString());
                contexts.add(new RecognitionContext(
                        SourceType.EMAIL,
                        config.sourceCode(),
                        fileName,
                        bodyPart.getContentType(),
                        Files.size(tempFile),
                        sender,
                        subject,
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

    private String senderOf(Message message) throws Exception {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) {
            return null;
        }
        return from[0].toString();
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
            int limit,
            String sourceCode
    ) {
        static EmailConfig from(TransferSource source) {
            Map<String, Object> config = source.connectionConfig() == null ? Map.of() : source.connectionConfig();
            String protocol = stringValue(config, "protocol", "imap");
            String host = requiredString(config, "host");
            int port = intValue(config, "port", "imaps".equalsIgnoreCase(protocol) ? 993 : 143);
            String username = requiredString(config, "username");
            String password = requiredString(config, "password");
            String folder = stringValue(config, "folder", "INBOX");
            boolean ssl = booleanValue(config, "ssl", "imaps".equalsIgnoreCase(protocol));
            int limit = intValue(config, "limit", 0);
            String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank() ? username : source.sourceCode();
            return new EmailConfig(protocol, host, port, username, password, folder, ssl, limit, sourceCode);
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
