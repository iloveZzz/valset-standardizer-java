package com.yss.valset.extract.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvValuationDataParserDynamicRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldKeepDefaultParseBehaviorWhenNoRuleStepConfigured() throws Exception {
        Path csv = createCsv(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10"),
                row("资产净值", "20")
        );
        CsvValuationDataParser parser = new CsvValuationDataParser(objectMapper);

        ParsedValuationData result = parser.parse(config(csv));

        assertThat(result.getHeaderRowNumber()).isEqualTo(2);
        assertThat(result.getDataStartRowNumber()).isEqualTo(3);
        assertThat(result.getSubjects()).hasSize(1);
        assertThat(result.getSubjects().get(0).getSubjectCode()).isEqualTo("1001");
        assertThat(result.getMetrics()).hasSize(1);
        assertThat(result.getMetrics().get(0).getMetricName()).isEqualTo("资产净值");
    }

    @Test
    void shouldUseSameDynamicRuleContractAsWorkbookParser() throws Exception {
        Path csv = createCsv(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("说明行", "有值"),
                row("1001", "银行存款", "10"),
                row("资产净值", "20")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .with(ParseRuleType.DATA_START, "rowIndex >= 3 && isSubjectRowWithPattern(row, subjectCodePattern)", "FAIL_FAST")
                .with(ParseRuleType.SUBJECT_EXTRACT, "{\"subjectCode\":\"CSV-\" + row[0],\"subjectName\":\"CSV动态科目\"}", "FAIL_FAST")
                .with(ParseRuleType.METRIC_EXTRACT, "{\"metricName\":\"CSV动态指标\",\"value\":\"888\",\"rawValues\":{\"value\":\"888\"}}", "FAIL_FAST");
        CsvValuationDataParser parser = new CsvValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(csv));

        assertThat(result.getDataStartRowNumber()).isEqualTo(4);
        assertThat(result.getSubjects()).hasSize(1);
        assertThat(result.getSubjects().get(0).getSubjectCode()).isEqualTo("CSV-1001");
        assertThat(result.getSubjects().get(0).getSubjectName()).isEqualTo("CSV动态科目");
        assertThat(result.getMetrics()).hasSize(1);
        assertThat(result.getMetrics().get(0).getMetricName()).isEqualTo("CSV动态指标");
        assertThat(result.getMetrics().get(0).getValue()).isEqualTo("888");
    }

    @Test
    void shouldUseDynamicRowClassifyRule() throws Exception {
        Path csv = createCsv(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10"),
                row("资产净值", "20")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .rowClassifyExpr("isSubjectRowWithPattern(row, subjectCodePattern) ? 'SUBJECT' : 'IGNORE'");
        CsvValuationDataParser parser = new CsvValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(csv));

        assertThat(result.getSubjects()).hasSize(1);
        assertThat(result.getMetrics()).isEmpty();
    }

    private static DataSourceConfig config(Path csv) {
        return DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build();
    }

    private static Path createCsv(List<String>... rows) throws Exception {
        Path path = Files.createTempFile("valuation-parser-", ".csv");
        StringBuilder content = new StringBuilder();
        for (List<String> row : rows) {
            if (content.length() > 0) {
                content.append('\n');
            }
            content.append(String.join(",", row));
        }
        Files.write(path, content.toString().getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private static List<String> row(String... values) {
        return java.util.Arrays.asList(values);
    }

    private static class TestRuleResolver implements ParseRuleTemplateResolver {

        private final Map<ParseRuleType, ParseRuleStepDescriptor> rules = new EnumMap<>(ParseRuleType.class);
        private String rowClassifyExpr;

        TestRuleResolver with(ParseRuleType ruleType, String expression, String errorPolicy) {
            rules.put(ruleType, ParseRuleStepDescriptor.builder()
                    .profileId(1L)
                    .profileCode("TEST_PROFILE")
                    .version("v1")
                    .ruleType(ruleType)
                    .stepName(ruleType.name())
                    .expression(expression)
                    .errorPolicy(errorPolicy)
                    .build());
            return this;
        }

        TestRuleResolver rowClassifyExpr(String rowClassifyExpr) {
            this.rowClassifyExpr = rowClassifyExpr;
            return this;
        }

        @Override
        public String resolveHeaderExpr(String fileScene, String fileTypeName) {
            return null;
        }

        @Override
        public String resolveRowClassifyExpr(String fileScene, String fileTypeName) {
            return rowClassifyExpr;
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
            return java.util.Arrays.asList("科目代码", "科目名称");
        }

        @Override
        public String resolveSubjectCodePattern(String fileScene, String fileTypeName) {
            return null;
        }

        @Override
        public ParseRuleStepDescriptor resolveRuleStep(String fileScene, String fileTypeName, ParseRuleType ruleType) {
            return rules.get(ruleType);
        }
    }
}
