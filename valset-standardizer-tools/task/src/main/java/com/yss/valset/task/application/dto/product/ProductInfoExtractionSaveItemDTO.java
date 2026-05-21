package com.yss.valset.task.application.dto.product;

import lombok.Data;

import java.io.Serializable;

/**
 * 产品信息提取保存明细结果。
 */
@Data
public class ProductInfoExtractionSaveItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transferId;
    private String originalName;
    private String productCode;
    private String productName;
    private String ruleId;
    private String status;
    private String message;
}
