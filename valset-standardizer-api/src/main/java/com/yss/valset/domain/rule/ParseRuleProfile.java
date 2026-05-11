package com.yss.valset.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 解析模板主实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRuleProfile {

    private Long id;

    /**
     * 模板编码。
     */
    private String profileCode;

    /**
     * 模板名称。
     */
    private String profileName;

    /**
     * 版本号。
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
     * 模板匹配表达式。
     */
    private String matchExpr;

    /**
     * 表头识别表达式。
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
     * 值转换表达式。
     */
    private String transformExpr;

    /**
     * 表头必选字段。
     */
    private List<String> requiredHeaders;

    /**
     * 科目代码正则表达式。
     */
    private String subjectCodePattern;

    /**
     * 是否开启表达式追踪。
     */
    private Boolean traceEnabled;

    /**
     * 超时时间毫秒数。
     */
    private Long timeoutMs;
}
