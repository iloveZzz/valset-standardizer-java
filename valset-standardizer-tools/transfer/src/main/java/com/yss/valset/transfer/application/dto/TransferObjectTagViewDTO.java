package com.yss.valset.transfer.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件主对象标签视图。
 */
@Data
@Builder
public class TransferObjectTagViewDTO {

    /**
     * 标签结果主键。
     */
    private String id;

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 标签主键。
     */
    private String tagId;

    /**
     * 标签编码。
     */
    private String tagCode;

    /**
     * 标签名称。
     */
    private String tagName;

    /**
     * 标签值。
     */
    private String tagValue;

    /**
     * 匹配策略。
     */
    private String matchStrategy;

    /**
     * 命中原因。
     */
    private String matchReason;

    /**
     * 命中字段。
     */
    private String matchedField;

    /**
     * 命中值。
     */
    private String matchedValue;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
