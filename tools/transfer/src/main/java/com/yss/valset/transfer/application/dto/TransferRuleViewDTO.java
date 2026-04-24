package com.yss.valset.transfer.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 路由规则视图。
 */
@Data
@Builder
public class TransferRuleViewDTO {

    /**
     * 规则主键。
     */
    private String ruleId;
    /**
     * 规则编码。
     */
    private String ruleCode;
    /**
     * 规则名称。
     */
    private String ruleName;
    /**
     * 规则版本号。
     */
    private String ruleVersion;
    /**
     * 是否启用。
     */
    private Boolean enabled;
    /**
     * 优先级。
     */
    private Integer priority;
    /**
     * 匹配策略。
     */
    private String matchStrategy;
    /**
     * 脚本语言。
     */
    private String scriptLanguage;
    /**
     * 规则脚本内容。
     */
    private String scriptBody;
    /**
     * 生效开始时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveFrom;
    /**
     * 生效结束时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime effectiveTo;
    /**
     * 规则扩展信息。
     */
    private Map<String, Object> ruleMeta;
    /**
     * 规则对应表单模板名。
     */
    private String formTemplateName;
}
