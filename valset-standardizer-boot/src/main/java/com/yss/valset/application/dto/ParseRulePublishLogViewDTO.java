package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析规则发布日志视图。
 */
@Data
@Builder
public class ParseRulePublishLogViewDTO {
    private Long id;
    private Long profileId;
    private String version;
    private String publishStatus;
    private LocalDateTime publishTime;
    private String publisher;
    private String publishComment;
    private String validationResultJson;
    private String rollbackFromVersion;
}
