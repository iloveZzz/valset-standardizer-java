package com.yss.valset.extract.rule;

import com.yss.valset.domain.rule.ParseRuleType;

import java.util.List;

/**
 * 解析模板运行时解析器。
 */
public interface ParseRuleTemplateResolver {

    /**
     * 解析表头规则表达式。
     */
    String resolveHeaderExpr(String fileScene, String fileTypeName);

    /**
     * 解析行分类规则表达式。
     */
    String resolveRowClassifyExpr(String fileScene, String fileTypeName);

    /**
     * 解析字段映射规则表达式。
     */
    String resolveFieldMapExpr(String fileScene, String fileTypeName);

    /**
     * 解析值转换规则表达式。
     */
    String resolveTransformExpr(String fileScene, String fileTypeName);

    /**
     * 解析表头必选字段。
     */
    List<String> resolveRequiredHeaders(String fileScene, String fileTypeName);

    /**
     * 解析科目代码正则表达式。
     */
    String resolveSubjectCodePattern(String fileScene, String fileTypeName);

    /**
     * 解析启用的规则步骤。
     */
    ParseRuleStepDescriptor resolveRuleStep(String fileScene, String fileTypeName, ParseRuleType ruleType);
}
