package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件收发运行日志状态统计视图。
 */
@Data
@Builder
public class TransferRunLogStatusCountViewDTO {

    /**
     * 运行状态。
     */
    private String runStatus;

    /**
     * 状态名称。
     */
    private String statusLabel;

    /**
     * 状态数量。
     */
    private Long count;
}
