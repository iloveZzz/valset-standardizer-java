package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析模板视图。
 */
@Data
@Builder
public class ParseRuleProfileViewDTO {
    private Long id;
    private String profileCode;
    private String profileName;
    private String version;
    private String fileScene;
    private String fileTypeName;
    private String sourceChannel;
    private String status;
    private Integer priority;
    private String matchExpr;
    private String headerExpr;
    private String rowClassifyExpr;
    private String fieldMapExpr;
    private String transformExpr;
    private Boolean traceEnabled;
    private Long timeoutMs;
    private String checksum;
    private LocalDateTime publishedTime;
    private Integer ruleCount;
    private Integer caseCount;
}
