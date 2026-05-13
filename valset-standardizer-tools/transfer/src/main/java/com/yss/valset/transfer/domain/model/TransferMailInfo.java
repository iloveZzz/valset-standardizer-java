package com.yss.valset.transfer.domain.model;

/**
 * 分拣对象关联的邮件信息。
 */
public class TransferMailInfo {

    private final String transferId;
    private final String mailId;
    private final String mailFrom;
    private final String mailTo;
    private final String mailCc;
    private final String mailBcc;
    private final String mailSubject;
    private final String mailBody;
    private final String mailProtocol;
    private final String mailFolder;

    public TransferMailInfo(String transferId, String mailId, String mailFrom, String mailTo, String mailCc, String mailBcc, String mailSubject, String mailBody, String mailProtocol, String mailFolder) {
        this.transferId = transferId;
        this.mailId = mailId;
        this.mailFrom = mailFrom;
        this.mailTo = mailTo;
        this.mailCc = mailCc;
        this.mailBcc = mailBcc;
        this.mailSubject = mailSubject;
        this.mailBody = mailBody;
        this.mailProtocol = mailProtocol;
        this.mailFolder = mailFolder;
    }



    public String transferId() {
        return transferId;
    }

    public String mailId() {
        return mailId;
    }

    public String mailFrom() {
        return mailFrom;
    }

    public String mailTo() {
        return mailTo;
    }

    public String mailCc() {
        return mailCc;
    }

    public String mailBcc() {
        return mailBcc;
    }

    public String mailSubject() {
        return mailSubject;
    }

    public String mailBody() {
        return mailBody;
    }

    public String mailProtocol() {
        return mailProtocol;
    }

    public String mailFolder() {
        return mailFolder;
    }



    public String getTransferId() {
        return transferId;
    }

    public String getMailId() {
        return mailId;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public String getMailCc() {
        return mailCc;
    }

    public String getMailBcc() {
        return mailBcc;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public String getMailBody() {
        return mailBody;
    }

    public String getMailProtocol() {
        return mailProtocol;
    }

    public String getMailFolder() {
        return mailFolder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferMailInfo other = (TransferMailInfo) o;
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        if (!java.util.Objects.equals(mailId, other.mailId)) {
            return false;
        }
        if (!java.util.Objects.equals(mailFrom, other.mailFrom)) {
            return false;
        }
        if (!java.util.Objects.equals(mailTo, other.mailTo)) {
            return false;
        }
        if (!java.util.Objects.equals(mailCc, other.mailCc)) {
            return false;
        }
        if (!java.util.Objects.equals(mailBcc, other.mailBcc)) {
            return false;
        }
        if (!java.util.Objects.equals(mailSubject, other.mailSubject)) {
            return false;
        }
        if (!java.util.Objects.equals(mailBody, other.mailBody)) {
            return false;
        }
        if (!java.util.Objects.equals(mailProtocol, other.mailProtocol)) {
            return false;
        }
        if (!java.util.Objects.equals(mailFolder, other.mailFolder)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(transferId, mailId, mailFrom, mailTo, mailCc, mailBcc, mailSubject, mailBody, mailProtocol, mailFolder);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferMailInfo[");
        sb.append("transferId=").append(transferId);
        sb.append(", mailId=").append(mailId);
        sb.append(", mailFrom=").append(mailFrom);
        sb.append(", mailTo=").append(mailTo);
        sb.append(", mailCc=").append(mailCc);
        sb.append(", mailBcc=").append(mailBcc);
        sb.append(", mailSubject=").append(mailSubject);
        sb.append(", mailBody=").append(mailBody);
        sb.append(", mailProtocol=").append(mailProtocol);
        sb.append(", mailFolder=").append(mailFolder);
        sb.append(']');
        return sb.toString();
    }



}
