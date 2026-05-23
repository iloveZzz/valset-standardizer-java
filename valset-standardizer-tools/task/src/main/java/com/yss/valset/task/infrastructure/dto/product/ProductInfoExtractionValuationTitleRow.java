package com.yss.valset.task.infrastructure.dto.product;

import lombok.Data;

/**
 * 产品信息提取使用的估值解析标题行。
 */
@Data
public class ProductInfoExtractionValuationTitleRow {

    private Long fileId;

    private String title;
}
