package com.yss.valset.task.application.dto.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 产品信息提取预览结果。
 */
@Data
public class ProductInfoExtractionPreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transferId;
    private String originalName;
    private String productType;
    private String subjectSystem;
    private String managerCode;
    private String managerName;
    private String holdingStatus;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate establishedDate;

    private String productCode;
    private String productName;
    private String matchRule;
    private String effectiveFrequency;
    private Integer delayDays;
    private Boolean approvalRequired;
}
