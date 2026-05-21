package com.yss.valset.task.infrastructure.dto.product;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品信息提取候选文件查询行。
 */
@Data
public class ProductInfoExtractionCandidateRow {

    private String transferId;
    private String originalName;
    private String sourceType;
    private String sourceCode;
    private String status;
    private String valuationTagName;
    private LocalDateTime receivedAt;
}
