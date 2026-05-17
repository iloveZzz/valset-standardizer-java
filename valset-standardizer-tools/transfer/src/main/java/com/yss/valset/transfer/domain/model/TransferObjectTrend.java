package com.yss.valset.transfer.domain.model;

/**
 * 分拣对象趋势统计。
 */
public class TransferObjectTrend {

    private final String trendDate;
    private final long deliveredCount;
    private final long undeliveredCount;

    public TransferObjectTrend(String trendDate, long deliveredCount, long undeliveredCount) {
        this.trendDate = trendDate;
        this.deliveredCount = deliveredCount;
        this.undeliveredCount = undeliveredCount;
    }

    public String trendDate() {
        return trendDate;
    }

    public long deliveredCount() {
        return deliveredCount;
    }

    public long undeliveredCount() {
        return undeliveredCount;
    }

    public String getTrendDate() {
        return trendDate;
    }

    public long getDeliveredCount() {
        return deliveredCount;
    }

    public long getUndeliveredCount() {
        return undeliveredCount;
    }
}
