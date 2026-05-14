package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件收发运行日志趋势视图。
 */
@Data
@Builder
public class TransferRunLogTrendViewDTO implements java.io.Serializable {

    /**
     * 趋势日期。
     */
    private String trendDate;

    /**
     * 当日数量。
     */
    private Long count;
}
