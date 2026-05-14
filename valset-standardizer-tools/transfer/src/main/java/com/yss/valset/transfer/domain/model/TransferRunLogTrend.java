package com.yss.valset.transfer.domain.model;

/**
 * 文件收发运行日志趋势统计。
 */
public class TransferRunLogTrend {

    private final String trendDate;
    private final long count;

    public TransferRunLogTrend(String trendDate, long count) {
        this.trendDate = trendDate;
        this.count = count;
    }

    public String trendDate() {
        return trendDate;
    }

    public long count() {
        return count;
    }

    public String getTrendDate() {
        return trendDate;
    }

    public long getCount() {
        return count;
    }
}
