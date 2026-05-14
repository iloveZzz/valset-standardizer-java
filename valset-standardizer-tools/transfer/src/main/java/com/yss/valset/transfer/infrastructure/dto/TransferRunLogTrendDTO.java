package com.yss.valset.transfer.infrastructure.dto;

/**
 * 文件收发运行日志趋势聚合结果。
 */
public class TransferRunLogTrendDTO implements java.io.Serializable {

    private String trendDate;
    private Long count;

    public String getTrendDate() {
        return trendDate;
    }

    public void setTrendDate(String trendDate) {
        this.trendDate = trendDate;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
