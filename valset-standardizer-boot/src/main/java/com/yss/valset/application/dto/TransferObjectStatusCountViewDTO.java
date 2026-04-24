package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件状态统计视图。
 */
@Data
@Builder
public class TransferObjectStatusCountViewDTO {

    /**
     * 文件状态。
     */
    private String status;

    /**
     * 状态名称。
     */
    private String statusLabel;

    /**
     * 数量。
     */
    private Long count;
}
