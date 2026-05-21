package com.yss.valset.extract.standardization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import com.yss.valset.extract.rule.QlexpressParseRuleEngine;
import com.yss.valset.extract.standardization.mapping.QlexpressHeaderMappingEngine;
import com.yss.valset.qlexpress.domain.runtime.QlexpressFunctionScript;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldAutoRegisterUnmappedColumnAndMetricSourceRows() throws Exception {
        FileParseSourceRepository sourceRepository = mock(FileParseSourceRepository.class);
        when(sourceRepository.selectList(any())).thenReturn(Collections.emptyList());

        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                objectMapper,
                null,
                sourceRepository,
                new QlexpressParseRuleEngine(objectMapper, ExternalValuationStandardizationServiceDynamicRuleTest::qlexpressScripts),
                new QlexpressHeaderMappingEngine(),
                new TestRuleResolver());

        ParsedValuationData data = ParsedValuationData.builder()
                .workbookPath("/tmp/valuation.xlsx")
                .fileNameOriginal("valuation.xlsx")
                .sheetName("ODS_RAW_DATA")
                .headers(java.util.Arrays.asList("科目代码", "未映射列"))
                .headerColumns(java.util.Arrays.asList(
                        HeaderColumnMeta.builder().columnIndex(0).headerName("科目代码").headerPath("科目代码").pathSegments(java.util.Collections.singletonList("科目代码")).build(),
                        HeaderColumnMeta.builder().columnIndex(1).headerName("未映射列").headerPath("未映射列").pathSegments(java.util.Collections.singletonList("未映射列")).build()))
                .metrics(java.util.Collections.singletonList(MetricRecord.builder()
                        .sheetName("ODS_RAW_DATA")
                        .rowDataNumber(4)
                        .metricName("未映射指标")
                        .metricType("metric_data")
                        .value("20")
                        .rawValues(com.yss.valset.common.support.Java8Maps.of("value", "20"))
                        .build()))
                .build();

        service.standardize(data);

        ArgumentCaptor<FileParseSourcePO> captor = ArgumentCaptor.forClass(FileParseSourcePO.class);
        verify(sourceRepository, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(FileParseSourcePO::getColumnName)
                .containsExactlyInAnyOrder("未映射列", "未映射指标");
        assertThat(captor.getAllValues()).extracting(FileParseSourcePO::getFileExtInfo)
                .containsExactlyInAnyOrder("{\"regionName\":\"column\"}", "{\"regionName\":\"metric\"}");
        assertThat(captor.getAllValues()).extracting(FileParseSourcePO::getStatus)
                .containsOnly(Boolean.FALSE);
    }

    private ExternalValuationStandardizationService service(ParseRuleTemplateResolver resolver) {
        return new ExternalValuationStandardizationService(
                objectMapper,
                null,
                null,
                new QlexpressParseRuleEngine(objectMapper, ExternalValuationStandardizationServiceDynamicRuleTest::qlexpressScripts),
                new QlexpressHeaderMappingEngine(),
                resolver);
    }

    private static List<QlexpressFunctionScript> qlexpressScripts() {
        return Collections.singletonList(new QlexpressFunctionScript(
                "mapOf",
                "function mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10) { m = {:}; if (k1 != null) { m[k1] = v1; }; if (k2 != null) { m[k2] = v2; }; if (k3 != null) { m[k3] = v3; }; if (k4 != null) { m[k4] = v4; }; if (k5 != null) { m[k5] = v5; }; if (k6 != null) { m[k6] = v6; }; if (k7 != null) { m[k7] = v7; }; if (k8 != null) { m[k8] = v8; }; if (k9 != null) { m[k9] = v9; }; if (k10 != null) { m[k10] = v10; }; return m; }",
                Collections.singletonList("extract.parse")
        ));
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
