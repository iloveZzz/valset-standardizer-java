package com.yss.valset.extract.standardization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import com.yss.valset.extract.rule.QlexpressParseRuleEngine;
import com.yss.valset.extract.standardization.mapping.QlexpressHeaderMappingEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalValuationStandardizationServiceDynamicRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldOverrideStandardizedSubjectAndMetricByTransformRuleMap() {
        TestRuleResolver resolver = new TestRuleResolver()
                .transformExpr("recordType == 'SUBJECT' ? "
                        + "mapOf('standardCode','QL_SUBJECT','standardName','QL科目','standardValues',mapOf('subject_code','QL_SUBJECT'),'mappingStatus','MAPPED','mappingReason','QL transform','mappingConfidence',0.88) "
                        + ": mapOf('standardCode','QL_METRIC','standardName','QL指标','standardValueText','123.45','standardValueNumber','123.45','standardValueUnit','元','standardValues',mapOf('metric_value','123.45'),'mappingStatus','MAPPED','mappingReason','QL transform','mappingConfidence',0.99)");
        ExternalValuationStandardizationService service = service(resolver);

        ParsedValuationData result = service.standardize(parsedData());

        SubjectRecord subject = result.getSubjects().get(0);
        assertThat(subject.getStandardCode()).isEqualTo("QL_SUBJECT");
        assertThat(subject.getStandardName()).isEqualTo("QL科目");
        assertThat(subject.getStandardValues()).containsEntry("subject_code", "QL_SUBJECT");
        assertThat(subject.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(subject.getMappingConfidence()).isEqualTo(0.88D);

        MetricRecord metric = result.getMetrics().get(0);
        assertThat(metric.getStandardCode()).isEqualTo("QL_METRIC");
        assertThat(metric.getStandardName()).isEqualTo("QL指标");
        assertThat(metric.getStandardValueText()).isEqualTo("123.45");
        assertThat(metric.getStandardValueNumber()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(metric.getStandardValueUnit()).isEqualTo("元");
        assertThat(metric.getStandardValues()).containsEntry("metric_value", "123.45");
        assertThat(metric.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(metric.getMappingConfidence()).isEqualTo(0.99D);
    }

    @Test
    void shouldKeepDefaultStandardizationWhenTransformRuleReturnsNonMap() {
        TestRuleResolver resolver = new TestRuleResolver()
                .transformExpr("'not-map'");
        ExternalValuationStandardizationService service = service(resolver);

        ParsedValuationData result = service.standardize(parsedData());

        SubjectRecord subject = result.getSubjects().get(0);
        assertThat(subject.getStandardCode()).isEqualTo("1001");
        assertThat(subject.getStandardName()).isEqualTo("银行存款");
        assertThat(subject.getMappingStatus()).isEqualTo("MAPPED");

        MetricRecord metric = result.getMetrics().get(0);
        assertThat(metric.getStandardCode()).isEqualTo("资产净值");
        assertThat(metric.getStandardName()).isEqualTo("资产净值");
        assertThat(metric.getStandardValueText()).isEqualTo("20");
        assertThat(metric.getMappingStatus()).isEqualTo("MAPPED");
    }

    private ExternalValuationStandardizationService service(ParseRuleTemplateResolver resolver) {
        return new ExternalValuationStandardizationService(
                objectMapper,
                null,
                null,
                new QlexpressParseRuleEngine(),
                new QlexpressHeaderMappingEngine(),
                resolver);
    }

    private ParsedValuationData parsedData() {
        return ParsedValuationData.builder()
                .workbookPath("/tmp/valuation.xlsx")
                .fileNameOriginal("valuation.xlsx")
                .sheetName("ODS_RAW_DATA")
                .headers(java.util.Arrays.asList("科目代码", "科目名称", "市值"))
                .headerColumns(java.util.Arrays.asList(
                        HeaderColumnMeta.builder().columnIndex(0).headerName("科目代码").headerPath("科目代码").pathSegments(java.util.Arrays.asList("科目代码")).build(),
                        HeaderColumnMeta.builder().columnIndex(1).headerName("科目名称").headerPath("科目名称").pathSegments(java.util.Arrays.asList("科目名称")).build(),
                        HeaderColumnMeta.builder().columnIndex(2).headerName("市值").headerPath("市值").pathSegments(java.util.Arrays.asList("市值")).build()))
                .subjects(java.util.Arrays.asList(SubjectRecord.builder()
                        .sheetName("ODS_RAW_DATA")
                        .rowDataNumber(3)
                        .subjectCode("1001")
                        .subjectName("银行存款")
                        .rawValues(java.util.Arrays.asList("1001", "银行存款", "10"))
                        .build()))
                .metrics(java.util.Arrays.asList(MetricRecord.builder()
                        .sheetName("ODS_RAW_DATA")
                        .rowDataNumber(4)
                        .metricName("资产净值")
                        .metricType("metric_data")
                        .value("20")
                        .rawValues(com.yss.valset.common.support.Java8Maps.of("value", "20"))
                        .build()))
                .build();
    }

    private static class TestRuleResolver implements ParseRuleTemplateResolver {

        private final Map<ParseRuleType, ParseRuleStepDescriptor> rules = new EnumMap<>(ParseRuleType.class);
        private String transformExpr;

        TestRuleResolver transformExpr(String transformExpr) {
            this.transformExpr = transformExpr;
            return this;
        }

        @Override
        public String resolveHeaderExpr(String fileScene, String fileTypeName) {
            return null;
        }

        @Override
        public String resolveRowClassifyExpr(String fileScene, String fileTypeName) {
            return null;
        }

        @Override
        public String resolveFieldMapExpr(String fileScene, String fileTypeName) {
            return null;
        }

        @Override
        public String resolveTransformExpr(String fileScene, String fileTypeName) {
            return transformExpr;
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
