package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件主对象按来源类型统计视图。
 */
@Data
@Builder
public class TransferObjectSourceAnalysisViewDTO {

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 总数。
     */
    private Long totalCount;

    /**
     * 状态统计列表。
     */
    private List<TransferObjectStatusCountViewDTO> statusCounts;
}
