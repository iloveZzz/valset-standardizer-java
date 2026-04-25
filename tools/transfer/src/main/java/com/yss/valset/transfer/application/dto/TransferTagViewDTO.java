package com.yss.valset.transfer.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 标签视图。
 */
@Data
@Builder
public class TransferTagViewDTO {

    private String tagId;

    private String tagCode;

    private String tagName;

    private String tagValue;

    private Boolean enabled;

    private Integer priority;

    private String matchStrategy;

    private String scriptLanguage;

    private String scriptBody;

    private String regexPattern;

    private Map<String, Object> tagMeta;

    private String formTemplateName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
