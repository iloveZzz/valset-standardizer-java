package com.yss.valset.application.command;

import lombok.Data;

import java.util.List;

/**
 * 解析模板新增或更新请求。
 */
@Data
public class ParseRuleProfileUpsertCommand {
    /**
     * 模板主键。
     */
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
    private Boolean traceEnabled = Boolean.FALSE;
    /**
     * 超时时间，单位毫秒。
     */
    private Long timeoutMs;
    /**
     * 创建人。
     */
    private String createdBy;
    /**
     * 修改人。
     */
    private String modifiedBy;
    /**
     * 规则步骤。
     */
    private List<ParseRuleDefinitionUpsertCommand> rules;
    /**
     * 回归样例。
     */
    private List<ParseRuleCaseUpsertCommand> cases;
}
