package com.yss.valset.transfer.domain.rule;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionRuleParserTest {

    @Test
    void shouldCompileAndEvaluateMailConditionExpression() {
        String json = """
                {
                  "id": "root",
                  "type": "GROUP",
                  "logicalOp": "AND",
                  "children": [
                    {
                      "id": "type",
                      "type": "LEAF",
                      "field": "attachmentFileType",
                      "operator": "CONTAINS",
                      "value": ["xls", "xlsx", "7z", "rar"]
                    },
                    {
                      "id": "limit",
                      "type": "LEAF",
                      "field": "limit",
                      "operator": "<=",
                      "value": "50"
                    }
                  ]
                }
                """;

        ConditionRule conditionRule = ConditionRuleParser.fromJsonString(json);
        String expression = conditionRule.toConditionRule();

        assertThat(expression).contains("containsAnyText(attachmentFileType, 'xls,xlsx,7z,rar')");
        assertThat(expression).contains("limit <= 50");

        ScriptRuleEngineAdapter engine = new ScriptRuleEngineAdapter();
        assertThat(engine.evaluateBooleanExpression(expression, Map.of(
                "attachmentFileType", "xlsx",
                "limit", 3
        ))).isTrue();
        assertThat(engine.evaluateBooleanExpression(expression, Map.of(
                "attachmentFileType", "pdf",
                "limit", 3
        ))).isFalse();
    }

    @Test
    void shouldCompileAndEvaluateMailSizeConditionExpression() {
        String json = """
                {
                  "id": "root",
                  "type": "LEAF",
                  "field": "attachmentSize",
                  "operator": "<=",
                  "value": 1048576
                }
                """;

        ConditionRule conditionRule = ConditionRuleParser.fromJsonString(json);
        String expression = conditionRule.toConditionRule();

        assertThat(expression).contains("attachmentSize <= 1048576");

        ScriptRuleEngineAdapter engine = new ScriptRuleEngineAdapter();
        assertThat(engine.evaluateBooleanExpression(expression, Map.of(
                "attachmentSize", 1024L
        ))).isTrue();
        assertThat(engine.evaluateBooleanExpression(expression, Map.of(
                "attachmentSize", 2097152L
        ))).isFalse();
    }

    @Test
    void shouldEvaluateRegisteredTextFunctions() {
        ScriptRuleEngineAdapter engine = new ScriptRuleEngineAdapter();

        assertThat(engine.evaluateBooleanExpression(
                "containsIgnoreCase(subject, '日报')",
                Map.of("subject", "周报日报汇总")
        )).isTrue();
        assertThat(engine.evaluateBooleanExpression(
                "containsAnyText(attachmentFileType, 'xls,xlsx,7z,rar')",
                Map.of("attachmentFileType", "xlsx")
        )).isTrue();
    }
}
