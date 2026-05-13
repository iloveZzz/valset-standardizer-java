package com.yss.valset.transfer.domain.model;

/**
 * 文件收发运行日志状态统计。
 */
public class TransferRunLogStatusCount {

    private final String runStatus;
    private final long count;

    public TransferRunLogStatusCount(String runStatus, long count) {
        this.runStatus = runStatus;
        this.count = count;
    }



    public String runStatus() {
        return runStatus;
    }

    public long count() {
        return count;
    }



    public String getRunStatus() {
        return runStatus;
    }

    public long getCount() {
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
        TransferRunLogStatusCount other = (TransferRunLogStatusCount) o;
        if (!java.util.Objects.equals(runStatus, other.runStatus)) {
            return false;
        }
        if (count != other.count) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(runStatus, count);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRunLogStatusCount[");
        sb.append("runStatus=").append(runStatus);
        sb.append(", count=").append(count);
        sb.append(']');
        return sb.toString();
    }



}
