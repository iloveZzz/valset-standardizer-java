package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 识别上下文，用于向插件和脚本规则暴露受控字段。
 */
public class RecognitionContext {

    private final SourceType sourceType;
    private final String sourceCode;
    private final String fileName;
    private final String mimeType;
    private final Long fileSize;
    private final String sender;
    private final String recipientsTo;
    private final String recipientsCc;
    private final String recipientsBcc;
    private final String subject;
    private final String body;
    private final String mailId;
    private final String mailProtocol;
    private final String mailFolder;
    private final String path;
    private final Map<String, Object> attributes;

    public RecognitionContext(SourceType sourceType, String sourceCode, String fileName, String mimeType, Long fileSize, String sender, String recipientsTo, String recipientsCc, String recipientsBcc, String subject, String body, String mailId, String mailProtocol, String mailFolder, String path, Map<String, Object> attributes) {
        this.sourceType = sourceType;
        this.sourceCode = sourceCode;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.sender = sender;
        this.recipientsTo = recipientsTo;
        this.recipientsCc = recipientsCc;
        this.recipientsBcc = recipientsBcc;
        this.subject = subject;
        this.body = body;
        this.mailId = mailId;
        this.mailProtocol = mailProtocol;
        this.mailFolder = mailFolder;
        this.path = path;
        this.attributes = attributes;
    }



    public SourceType sourceType() {
        return sourceType;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String fileName() {
        return fileName;
    }

    public String mimeType() {
        return mimeType;
    }

    public Long fileSize() {
        return fileSize;
    }

    public String sender() {
        return sender;
    }

    public String recipientsTo() {
        return recipientsTo;
    }

    public String recipientsCc() {
        return recipientsCc;
    }

    public String recipientsBcc() {
        return recipientsBcc;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public String mailId() {
        return mailId;
    }

    public String mailProtocol() {
        return mailProtocol;
    }

    public String mailFolder() {
        return mailFolder;
    }

    public String path() {
        return path;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }



    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipientsTo() {
        return recipientsTo;
    }

    public String getRecipientsCc() {
        return recipientsCc;
    }

    public String getRecipientsBcc() {
        return recipientsBcc;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getMailId() {
        return mailId;
    }

    public String getMailProtocol() {
        return mailProtocol;
    }

    public String getMailFolder() {
        return mailFolder;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecognitionContext other = (RecognitionContext) o;
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        if (!java.util.Objects.equals(fileName, other.fileName)) {
            return false;
        }
        if (!java.util.Objects.equals(mimeType, other.mimeType)) {
            return false;
        }
        if (!java.util.Objects.equals(fileSize, other.fileSize)) {
            return false;
        }
        if (!java.util.Objects.equals(sender, other.sender)) {
            return false;
        }
        if (!java.util.Objects.equals(recipientsTo, other.recipientsTo)) {
            return false;
        }
        if (!java.util.Objects.equals(recipientsCc, other.recipientsCc)) {
            return false;
        }
        if (!java.util.Objects.equals(recipientsBcc, other.recipientsBcc)) {
            return false;
        }
        if (!java.util.Objects.equals(subject, other.subject)) {
            return false;
        }
        if (!java.util.Objects.equals(body, other.body)) {
            return false;
        }
        if (!java.util.Objects.equals(mailId, other.mailId)) {
            return false;
        }
        if (!java.util.Objects.equals(mailProtocol, other.mailProtocol)) {
            return false;
        }
        if (!java.util.Objects.equals(mailFolder, other.mailFolder)) {
            return false;
        }
        if (!java.util.Objects.equals(path, other.path)) {
            return false;
        }
        if (!java.util.Objects.equals(attributes, other.attributes)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(sourceType, sourceCode, fileName, mimeType, fileSize, sender, recipientsTo, recipientsCc, recipientsBcc, subject, body, mailId, mailProtocol, mailFolder, path, attributes);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RecognitionContext[");
        sb.append("sourceType=").append(sourceType);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", fileName=").append(fileName);
        sb.append(", mimeType=").append(mimeType);
        sb.append(", fileSize=").append(fileSize);
        sb.append(", sender=").append(sender);
        sb.append(", recipientsTo=").append(recipientsTo);
        sb.append(", recipientsCc=").append(recipientsCc);
        sb.append(", recipientsBcc=").append(recipientsBcc);
        sb.append(", subject=").append(subject);
        sb.append(", body=").append(body);
        sb.append(", mailId=").append(mailId);
        sb.append(", mailProtocol=").append(mailProtocol);
        sb.append(", mailFolder=").append(mailFolder);
        sb.append(", path=").append(path);
        sb.append(", attributes=").append(attributes);
        sb.append(']');
        return sb.toString();
    }



}
