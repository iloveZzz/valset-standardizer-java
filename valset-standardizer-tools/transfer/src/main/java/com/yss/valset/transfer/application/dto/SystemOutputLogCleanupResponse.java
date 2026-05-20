package com.yss.valset.transfer.application.dto;

/**
 * 系统输出日志清理结果。
 */
public class SystemOutputLogCleanupResponse implements java.io.Serializable {

    private long deletedCount;

    private long remainingCount;

    public SystemOutputLogCleanupResponse() {
    }

    public SystemOutputLogCleanupResponse(long deletedCount, long remainingCount) {
        this.deletedCount = deletedCount;
        this.remainingCount = remainingCount;
    }

    public long getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(long deletedCount) {
        this.deletedCount = deletedCount;
    }

    public long getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(long remainingCount) {
        this.remainingCount = remainingCount;
    }
}
