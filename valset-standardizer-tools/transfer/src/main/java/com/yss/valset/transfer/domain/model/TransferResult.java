package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 投递执行结果。
 */
public class TransferResult {

    private final boolean success;
    private final String fileId;
    private final String storagePath;
    private final List<String> messages;

    public TransferResult(boolean success, String fileId, String storagePath, List<String> messages) {
        this.success = success;
        this.fileId = fileId;
        this.storagePath = storagePath;
        this.messages = messages;
    }

    public TransferResult(boolean success, String fileId, List<String> messages) {
        this(success, fileId, null, messages);
    }



    public boolean success() {
        return success;
    }

    public String fileId() {
        return fileId;
    }

    public String storagePath() {
        return storagePath;
    }

    public List<String> messages() {
        return messages;
    }



    public boolean getSuccess() {
        return success;
    }

    public String getFileId() {
        return fileId;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public List<String> getMessages() {
        return messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferResult other = (TransferResult) o;
        if (success != other.success) {
            return false;
        }
        if (!java.util.Objects.equals(fileId, other.fileId)) {
            return false;
        }
        if (!java.util.Objects.equals(storagePath, other.storagePath)) {
            return false;
        }
        if (!java.util.Objects.equals(messages, other.messages)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(success, fileId, storagePath, messages);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferResult[");
        sb.append("success=").append(success);
        sb.append(", fileId=").append(fileId);
        sb.append(", storagePath=").append(storagePath);
        sb.append(", messages=").append(messages);
        sb.append(']');
        return sb.toString();
    }

}
