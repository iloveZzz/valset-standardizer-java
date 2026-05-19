package com.yss.valset.extract.rule;

import com.yss.valset.domain.rule.ParseRuleType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置解析规则模板。
 *
 * <p>
 * 核心解析任务只依赖稳定的默认规则，不再在运行时查询解析模板主表或步骤表。
 * 解析规则管理能力可以继续维护模板数据，但不会影响批量解析主链路。
 * </p>
 */
@Component
public class DefaultParseRuleTemplateResolver implements ParseRuleTemplateResolver {

    private static final List<String> FALLBACK_REQUIRED_HEADERS = java.util.Arrays.asList("科目代码", "科目名称");

    @Override
    public String resolveHeaderExpr(String fileScene, String fileTypeName) {
        return ParseRuleExpressions.HEADER_ROW_EXPR;
    }

    @Override
    public String resolveRowClassifyExpr(String fileScene, String fileTypeName) {
        return ParseRuleExpressions.ROW_CLASSIFY_EXPR;
    }

    @Override
    public String resolveFieldMapExpr(String fileScene, String fileTypeName) {
        return null;
    }

    @Override
    public String resolveTransformExpr(String fileScene, String fileTypeName) {
        return null;
    }

    @Override
    public List<String> resolveRequiredHeaders(String fileScene, String fileTypeName) {
        return FALLBACK_REQUIRED_HEADERS;
    }

    @Override
    public String resolveSubjectCodePattern(String fileScene, String fileTypeName) {
        return null;
    }

    @Override
    public ParseRuleStepDescriptor resolveRuleStep(String fileScene, String fileTypeName, ParseRuleType ruleType) {
        return null;
    }
}
