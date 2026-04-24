package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 解析模板视图。
 */
@Data
@Builder
public class ParseRuleProfileViewDTO {
    /**
     * 模板主键。
     */
    private String id;
    /**
     * 模板编码。
     */
    private String profileCode;
    /**
     * 模板名称。
     */
    private String profileName;
    /**
     * 模板版本。
     */
    private String version;
    /**
     * 文件场景。
     */
    private String fileScene;
    /**
     * 文件类型名称。
     */
    private String fileTypeName;
    /**
     * 来源渠道。
     */
    private String sourceChannel;
    /**
     * 模板状态。
     */
    private String status;
    /**
     * 优先级。
     */
    private Integer priority;
    /**
     * 匹配表达式。
     */
    private String matchExpr;
    /**
     * 表头表达式。
     */
    private String headerExpr;
    /**
     * 行分类表达式。
     */
    private String rowClassifyExpr;
    /**
     * 字段映射表达式。
     */
    private String fieldMapExpr;
    /**
     * 转换表达式。
     */
    private String transformExpr;
    /**
     * 必填表头列表。
     */
    private List<String> requiredHeaders;
    /**
     * 科目编码正则。
     */
    private String subjectCodePattern;
    /**
     * 是否开启规则追踪，默认关闭。
     */
    private Boolean traceEnabled;
    /**
     * 超时时间，单位毫秒。
     */
    private String timeoutMs;
    /**
     * 校验和。
     */
    private String checksum;
    /**
     * 发布时间。
     */
    private LocalDateTime publishedTime;
    /**
     * 规则步骤数量。
     */
    private Integer ruleCount;
    /**
     * 样例数量。
     */
    private Integer caseCount;
}
