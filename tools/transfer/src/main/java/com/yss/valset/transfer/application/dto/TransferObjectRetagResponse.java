package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件主对象重新打标结果。
 */
@Data
@Builder
public class TransferObjectRetagResponse {

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
     * 命中标签总数。
     */
    private Integer matchedTagCount;

    /**
     * 处理明细。
     */
    private List<TransferObjectRetagItemViewDTO> items;
}
