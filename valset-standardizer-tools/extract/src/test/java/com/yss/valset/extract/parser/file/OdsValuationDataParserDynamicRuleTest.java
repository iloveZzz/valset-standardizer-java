package com.yss.valset.extract.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OdsValuationDataParserDynamicRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldKeepDefaultParseBehaviorWhenNoRuleStepConfigured() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10"),
                row("资产净值", "20")
        );
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper);

        ParsedValuationData result = parser.parse(config(workbook));

        assertThat(result.getHeaderRowNumber()).isEqualTo(2);
        assertThat(result.getDataStartRowNumber()).isEqualTo(3);
        assertThat(result.getSubjects()).hasSize(1);
        assertThat(result.getSubjects().get(0).getSubjectCode()).isEqualTo("1001");
        assertThat(result.getMetrics()).hasSize(1);
        assertThat(result.getMetrics().get(0).getMetricName()).isEqualTo("资产净值");
    }

    @Test
    void shouldUseDynamicDataStartRule() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("非数据说明", "有值"),
                row("1001", "银行存款", "10")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .with(ParseRuleType.DATA_START, "rowIndex >= 3 && isSubjectRowWithPattern(row, subjectCodePattern)", "FAIL_FAST");
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(workbook));

        assertThat(result.getDataStartRowNumber()).isEqualTo(4);
        assertThat(result.getSubjects()).hasSize(1);
    }

    @Test
    void shouldOverrideSubjectAndMetricExtractionByRuleMap() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10"),
                row("资产净值", "20")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .with(ParseRuleType.SUBJECT_EXTRACT, "{\"subjectCode\":\"S-\" + row[0],\"subjectName\":\"动态科目\"}", "FAIL_FAST")
                .with(ParseRuleType.METRIC_EXTRACT, "{\"metricName\":\"动态指标\",\"value\":\"999\",\"rawValues\":{\"value\":\"999\"}}", "FAIL_FAST");
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(workbook));

        assertThat(result.getSubjects().get(0).getSubjectCode()).isEqualTo("S-1001");
        assertThat(result.getSubjects().get(0).getSubjectName()).isEqualTo("动态科目");
        assertThat(result.getMetrics().get(0).getMetricName()).isEqualTo("动态指标");
        assertThat(result.getMetrics().get(0).getValue()).isEqualTo("999");
    }

    @Test
    void shouldFallbackToDefaultWhenExtractRuleReturnsNonMap() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .with(ParseRuleType.SUBJECT_EXTRACT, "'not-map'", "FALLBACK_DEFAULT");
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(workbook));

        assertThat(result.getSubjects()).hasSize(1);
        assertThat(result.getSubjects().get(0).getSubjectCode()).isEqualTo("1001");
        assertThat(result.getSubjects().get(0).getSubjectName()).isEqualTo("银行存款");
    }

    @Test
    void shouldUseDynamicHeaderAndRowClassifyRules() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目简称", "市值"),
                row("1001", "银行存款", "10"),
                row("资产净值", "20")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .requiredHeaders(java.util.Arrays.asList("科目代码", "科目简称"))
                .headerExpr("rowContainsAll(row, requiredHeaders) && row[1] == '科目简称'")
                .rowClassifyExpr("isSubjectRowWithPattern(row, subjectCodePattern) ? 'SUBJECT' : (firstMeaningfulText(row) == '资产净值' ? 'IGNORE' : classifyRowWithPattern(row, footerKeywords, subjectCodePattern))");
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(workbook));

        assertThat(result.getHeaderRowNumber()).isEqualTo(2);
        assertThat(result.getSubjects()).hasSize(1);
        assertThat(result.getMetrics()).isEmpty();
    }

    @Test
    void shouldSkipRowWhenExtractRuleFailsWithSkipPolicy() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .with(ParseRuleType.SUBJECT_EXTRACT, "unknownFunction(row)", "SKIP_ROW");
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper, resolver);

        ParsedValuationData result = parser.parse(config(workbook));

        assertThat(result.getSubjects()).isEmpty();
    }

    @Test
    void shouldPropagateRuleFailureWhenPolicyIsFailFast() throws Exception {
        Path workbook = createWorkbook(
                row("估值表"),
                row("科目代码", "科目名称", "市值"),
                row("1001", "银行存款", "10")
        );
        TestRuleResolver resolver = new TestRuleResolver()
                .with(ParseRuleType.SUBJECT_EXTRACT, "unknownFunction(row)", "FAIL_FAST");
        OdsValuationDataParser parser = new OdsValuationDataParser(objectMapper, resolver);

        assertThatThrownBy(() -> parser.parse(config(workbook)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QLExpress 规则执行失败");
    }

    private static DataSourceConfig config(Path workbook) {
        return DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri(workbook.toString())
                .build();
    }

    private static Path createWorkbook(List<String>... rows) throws Exception {
        Path path = Files.createTempFile("valuation-parser-", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("估值表");
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                List<String> values = rows[rowIndex];
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(values.get(columnIndex));
                }
            }
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            }
        }
        return path;
    }

    private static List<String> row(String... values) {
        return java.util.Arrays.asList(values);
    }

    private static class TestRuleResolver implements ParseRuleTemplateResolver {

        private final Map<ParseRuleType, ParseRuleStepDescriptor> rules = new EnumMap<>(ParseRuleType.class);
        private List<String> requiredHeaders = java.util.Arrays.asList("科目代码", "科目名称");
        private String subjectCodePattern;
        private String headerExpr;
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

        TestRuleResolver requiredHeaders(List<String> requiredHeaders) {
            this.requiredHeaders = requiredHeaders;
            return this;
        }

        TestRuleResolver subjectCodePattern(String subjectCodePattern) {
            this.subjectCodePattern = subjectCodePattern;
            return this;
        }

        TestRuleResolver headerExpr(String headerExpr) {
            this.headerExpr = headerExpr;
            return this;
        }

        TestRuleResolver rowClassifyExpr(String rowClassifyExpr) {
            this.rowClassifyExpr = rowClassifyExpr;
            return this;
        }

        @Override
        public String resolveHeaderExpr(String fileScene, String fileTypeName) {
            return headerExpr;
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
            return requiredHeaders;
        }

        @Override
        public String resolveSubjectCodePattern(String fileScene, String fileTypeName) {
            return subjectCodePattern;
        }

        @Override
        public ParseRuleStepDescriptor resolveRuleStep(String fileScene, String fileTypeName, ParseRuleType ruleType) {
            return rules.get(ruleType);
        }
    }
}
