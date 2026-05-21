package com.yss.valset.task.application.dto.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产品信息提取候选文件。
 */
@Data
public class ProductInfoExtractionCandidateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transferId;
    private String originalName;
    private String sourceType;
    private String sourceCode;
    private String status;
    private String deliveryStatus;
    private String valuationTagName;
    private String receiveMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedAt;
}
