package com.yss.valset.transfer.infrastructure.dto;

/**
 * 分拣对象趋势查询 DTO。
 */
public class TransferObjectTrendDTO implements java.io.Serializable {

    private String trendDate;
    private Long deliveredCount;
    private Long undeliveredCount;

    public String getTrendDate() {
        return trendDate;
    }

    public void setTrendDate(String trendDate) {
        this.trendDate = trendDate;
    }

    public Long getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(Long deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public Long getUndeliveredCount() {
        return undeliveredCount;
    }

    public void setUndeliveredCount(Long undeliveredCount) {
        this.undeliveredCount = undeliveredCount;
    }
}
