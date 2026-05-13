package com.yss.valset.parser.application.dto;

import com.yss.valset.parser.application.command.ParseRuleCaseUpsertCommand;
import com.yss.valset.parser.application.command.ParseRuleDefinitionUpsertCommand;
import com.yss.valset.parser.application.command.ParseRuleProfileUpsertCommand;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 解析模板导入导出包。
 */
@Data
@Builder
public class ParseRuleBundleExportDTO implements java.io.Serializable {

    /**
     * 包类型。
     */
    private String bundleType;

    /**
     * 包版本。
     */
    private String bundleVersion;

    /**
     * 导出时间。
     */
    private LocalDateTime exportedAt;

    /**
     * 模板主数据。
     */
    private ParseRuleProfileUpsertCommand profile;

    /**
     * 规则步骤。
     */
    private List<ParseRuleDefinitionUpsertCommand> rules;

    /**
     * 回归样例。
     */
    private List<ParseRuleCaseUpsertCommand> cases;

    /**
     * 发布日志。
     */
    private List<ParseRulePublishLogViewDTO> publishLogs;
}
