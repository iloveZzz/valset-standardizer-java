package com.yss.valset.transfer.domain.model;

/**
 * 邮件文件夹统计项。
 */
public class TransferObjectMailFolderCount {

    private final String mailFolder;
    private final Long count;

    public TransferObjectMailFolderCount(String mailFolder, Long count) {
        this.mailFolder = mailFolder;
        this.count = count;
    }



    public String mailFolder() {
        return mailFolder;
    }

    public Long count() {
        return count;
    }



    public String getMailFolder() {
        return mailFolder;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferObjectMailFolderCount other = (TransferObjectMailFolderCount) o;
        if (!java.util.Objects.equals(mailFolder, other.mailFolder)) {
            return false;
        }
        if (!java.util.Objects.equals(count, other.count)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(mailFolder, count);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectMailFolderCount[");
        sb.append("mailFolder=").append(mailFolder);
        sb.append(", count=").append(count);
        sb.append(']');
        return sb.toString();
    }



}
