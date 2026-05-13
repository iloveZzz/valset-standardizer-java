package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件投递结果统计视图。
 */
@Data
@Builder
public class TransferDeliveryRecordSummaryViewDTO implements java.io.Serializable{

    /**
     * 今日投递总数。
     */
    private Long todayDeliveryCount;

    /**
     * 今日成功投递数。
     */
    private Long todaySuccessCount;

    /**
     * 今日失败投递数。
     */
    private Long todayFailedCount;

    /**
     * 今日成功率，保留一位小数。
     */
    private Double successRate;
}
