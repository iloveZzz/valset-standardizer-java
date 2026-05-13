package com.yss.valset.transfer.domain.model;

/**
 * 文件后缀统计项。
 */
public class TransferObjectExtensionCount {

    private final String extension;
    private final Long count;

    public TransferObjectExtensionCount(String extension, Long count) {
        this.extension = extension;
        this.count = count;
    }



    public String extension() {
        return extension;
    }

    public Long count() {
        return count;
    }



    public String getExtension() {
        return extension;
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
        TransferObjectExtensionCount other = (TransferObjectExtensionCount) o;
        if (!java.util.Objects.equals(extension, other.extension)) {
            return false;
        }
        if (!java.util.Objects.equals(count, other.count)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(extension, count);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectExtensionCount[");
        sb.append("extension=").append(extension);
        sb.append(", count=").append(count);
        sb.append(']');
        return sb.toString();
    }



}
