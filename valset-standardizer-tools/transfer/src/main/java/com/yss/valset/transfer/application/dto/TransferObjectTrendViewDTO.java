package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件分拣对象趋势视图。
 */
@Data
@Builder
public class TransferObjectTrendViewDTO implements java.io.Serializable {

    /**
     * 趋势日期。
     */
    private String trendDate;

    /**
     * 已投递数量。
     */
    private Long deliveredCount;

    /**
     * 未投递数量。
     */
    private Long undeliveredCount;
}
