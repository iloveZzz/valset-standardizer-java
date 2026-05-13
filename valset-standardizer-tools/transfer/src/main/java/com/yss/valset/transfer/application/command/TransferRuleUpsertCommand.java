package com.yss.valset.transfer.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 路由规则新增或更新命令。
 */
@Data
public class TransferRuleUpsertCommand {

    /**
     * 规则主键。
     */
    private String ruleId;

    /**
     * 规则编码。
     */
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    /**
     * 规则名称。
     */
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /**
     * 规则版本号。
     */
    private String ruleVersion = "1.0.0";

    /**
     * 是否启用。
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * 优先级，数值越小优先级越高。
     */
    private Integer priority = 10;

    /**
     * 匹配策略。
     */
    private String matchStrategy = "SCRIPT_RULE";

    /**
     * 脚本语言。
     */
    private String scriptLanguage = "qlexpress4";

    /**
     * 规则脚本内容。
     */
    private String scriptBody;

    /**
     * 生效开始时间。
     */
    private LocalDateTime effectiveFrom;

    /**
     * 生效结束时间。
     */
    private LocalDateTime effectiveTo;

    /**
     * 规则扩展信息。
     */
    private Map<String, Object> ruleMeta;
}
