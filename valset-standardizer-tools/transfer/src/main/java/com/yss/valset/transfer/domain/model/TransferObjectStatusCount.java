package com.yss.valset.transfer.domain.model;

/**
 * 文件状态统计项。
 */
public class TransferObjectStatusCount {

    private final String status;
    private final Long count;

    public TransferObjectStatusCount(String status, Long count) {
        this.status = status;
        this.count = count;
    }



    public String status() {
        return status;
    }

    public Long count() {
        return count;
    }



    public String getStatus() {
        return status;
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
        TransferObjectStatusCount other = (TransferObjectStatusCount) o;
        if (!java.util.Objects.equals(status, other.status)) {
            return false;
        }
        if (!java.util.Objects.equals(count, other.count)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, count);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectStatusCount[");
        sb.append("status=").append(status);
        sb.append(", count=").append(count);
        sb.append(']');
        return sb.toString();
    }



}
