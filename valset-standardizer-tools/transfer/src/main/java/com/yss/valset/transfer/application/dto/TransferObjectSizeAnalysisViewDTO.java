package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件大小统计视图。
 */
@Data
@Builder
public class TransferObjectSizeAnalysisViewDTO {

    /**
     * 文件总数。
     */
    private Long totalCount;

    /**
     * 总大小，单位字节。
     */
    private Long totalSizeBytes;

    /**
     * 后缀统计列表。
     */
    private List<TransferObjectExtensionCountViewDTO> extensionCounts;
}
