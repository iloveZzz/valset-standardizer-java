package com.yss.valset.application.command;

import lombok.Data;

import java.util.List;

/**
 * 解析模板新增或更新请求。
 */
@Data
public class ParseRuleProfileUpsertCommand {
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
    private List<String> requiredHeaders;
    private String subjectCodePattern;
    /**
     * 是否开启规则追踪，默认关闭。
     */
    private Boolean traceEnabled = Boolean.FALSE;
    private Long timeoutMs;
    private String createdBy;
    private String modifiedBy;
    private List<ParseRuleDefinitionUpsertCommand> rules;
    private List<ParseRuleCaseUpsertCommand> cases;
}
