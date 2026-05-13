package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件主对象重新投递结果。
 */
@Data
@Builder
public class TransferObjectRedeliverResponse {

    /**
     * 请求数量。
     */
    private Integer requestedCount;

    /**
     * 成功数量。
     */
    private Integer successCount;

    /**
     * 失败数量。
     */
    private Integer failureCount;

    /**
     * 跳过数量。
     */
    private Integer skippedCount;

    /**
     * 处理明细。
     */
    private List<TransferObjectRedeliverItemViewDTO> items;
}
