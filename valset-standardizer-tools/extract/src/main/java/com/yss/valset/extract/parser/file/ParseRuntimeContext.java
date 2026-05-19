package com.yss.valset.extract.parser.file;

import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleExpressions;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;

import java.util.List;

/**
 * 单次估值文件解析使用的规则运行时上下文。
 */
final class ParseRuntimeContext {

    private static final List<String> FALLBACK_REQUIRED_HEADERS = java.util.Arrays.asList("科目代码", "科目名称");

    private final String fileScene;
    private final String fileTypeName;
    private final String headerExpr;
    private final String rowClassifyExpr;
    private final List<String> requiredHeaders;
    private final String subjectCodePattern;
    private final ParseRuleStepDescriptor dataStartRule;
    private final ParseRuleStepDescriptor subjectExtractRule;
    private final ParseRuleStepDescriptor metricExtractRule;

    private ParseRuntimeContext(
            String fileScene,
            String fileTypeName,
            String headerExpr,
            String rowClassifyExpr,
            List<String> requiredHeaders,
            String subjectCodePattern,
            ParseRuleStepDescriptor dataStartRule,
            ParseRuleStepDescriptor subjectExtractRule,
            ParseRuleStepDescriptor metricExtractRule
    ) {
        this.fileScene = fileScene;
        this.fileTypeName = fileTypeName;
        this.headerExpr = headerExpr;
        this.rowClassifyExpr = rowClassifyExpr;
        this.requiredHeaders = requiredHeaders;
        this.subjectCodePattern = subjectCodePattern;
        this.dataStartRule = dataStartRule;
        this.subjectExtractRule = subjectExtractRule;
        this.metricExtractRule = metricExtractRule;
    }

    static ParseRuntimeContext resolve(String fileScene, String fileTypeName, ParseRuleTemplateResolver resolver) {
        if (resolver == null) {
            return new ParseRuntimeContext(
                    fileScene,
                    fileTypeName,
                    null,
                    null,
                    FALLBACK_REQUIRED_HEADERS,
                    null,
                    null,
                    null,
                    null
            );
        }
        List<String> requiredHeaders = resolver.resolveRequiredHeaders(fileScene, fileTypeName);
        String subjectCodePattern = resolver.resolveSubjectCodePattern(fileScene, fileTypeName);
        return new ParseRuntimeContext(
                fileScene,
                fileTypeName,
                defaultExpression(resolver.resolveHeaderExpr(fileScene, fileTypeName), ParseRuleExpressions.HEADER_ROW_EXPR),
                defaultExpression(resolver.resolveRowClassifyExpr(fileScene, fileTypeName), ParseRuleExpressions.ROW_CLASSIFY_EXPR),
                requiredHeaders == null || requiredHeaders.isEmpty() ? FALLBACK_REQUIRED_HEADERS : requiredHeaders,
                subjectCodePattern == null || subjectCodePattern.trim().isEmpty() ? null : subjectCodePattern.trim(),
                resolver.resolveRuleStep(fileScene, fileTypeName, ParseRuleType.DATA_START),
                resolver.resolveRuleStep(fileScene, fileTypeName, ParseRuleType.SUBJECT_EXTRACT),
                resolver.resolveRuleStep(fileScene, fileTypeName, ParseRuleType.METRIC_EXTRACT)
        );
    }

    private static String defaultExpression(String expression, String fallback) {
        return expression == null || expression.trim().isEmpty() ? fallback : expression.trim();
    }

    String fileScene() {
        return fileScene;
    }

    String fileTypeName() {
        return fileTypeName;
    }

    String headerExpr() {
        return headerExpr;
    }

    String rowClassifyExpr() {
        return rowClassifyExpr;
    }

    List<String> requiredHeaders() {
        return requiredHeaders;
    }

    String subjectCodePattern() {
        return subjectCodePattern;
    }

    ParseRuleStepDescriptor dataStartRule() {
        return dataStartRule;
    }

    ParseRuleStepDescriptor subjectExtractRule() {
        return subjectExtractRule;
    }

    ParseRuleStepDescriptor metricExtractRule() {
        return metricExtractRule;
    }
}
