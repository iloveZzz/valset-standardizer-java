package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件收发运行日志批量重投递返回结果。
 */
@Data
@Builder
public class TransferRunLogRedeliverResponse {

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
     * 执行明细。
     */
    private List<TransferRunLogRedeliverItemViewDTO> items;
}
