package com.yss.valset.extract.rule;

import com.yss.valset.common.support.ExcelParsingSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QlexpressParseRuleEngineTest {

    @Test
    void shouldEvaluateBasicExpression() {
        QlexpressParseRuleEngine engine = new QlexpressParseRuleEngine();

        Object result = engine.evaluate("a + b * c", Map.of("a", 1, "b", 2, "c", 3));

        assertEquals(7, result);
    }

    @Test
    void shouldDetectHeaderAndClassifyRows() {
        QlexpressParseRuleEngine engine = new QlexpressParseRuleEngine();
        List<Object> headerRow = List.of("科目代码", "科目名称", "市值");
        List<Object> subjectRow = List.of("1102", "股票投资", "");
        List<Object> metricRow = List.of("市值", "", "1000");

        assertEquals("SUBJECT", engine.classifyRow(subjectRow, List.of("制表", "复核", "打印", "备注")));
        assertEquals("METRIC_ROW", engine.classifyRow(metricRow, List.of("制表", "复核", "打印", "备注")));
    }

    @Test
    void shouldEvaluateHasTextSafely() {
        QlexpressParseRuleEngine engine = new QlexpressParseRuleEngine();
        Map<String, Object> nullMetricNameContext = new HashMap<>();
        nullMetricNameContext.put("metricName", null);

        assertTrue(engine.evaluateBoolean("hasText(metricName)", Map.of("metricName", new StringBuilder("损益平准金"))));
        assertTrue(!engine.evaluateBoolean("hasText(metricName)", Map.of("metricName", "")));
        assertTrue(!engine.evaluateBoolean("hasText(metricName)", nullMetricNameContext));
    }
}
