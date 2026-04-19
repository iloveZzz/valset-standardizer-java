package com.yss.valset.extract.standardization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.entity.FileParseRulePO;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.mapper.FileParseRuleRepository;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalValuationStandardizationServiceTest {

    @Test
    void should_match_standard_fields_from_multilevel_chinese_headers_without_over_normalizing() {
        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称"),
                        rule(3L, "n_hldamt", "数量")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称"),
                        source(3L, "n_hldamt", "数量")
                ))
        );

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .headers(List.of("基础信息|科目代码", "基础信息|科目名称", "持仓信息|数量"))
                .headerColumns(List.of(
                        headerColumn(0, "基础信息|科目代码"),
                        headerColumn(1, "基础信息|科目名称"),
                        headerColumn(2, "持仓信息|数量")
                ))
                .basicInfo(Map.of("biz_date", "2023-03-21"))
                .subjects(List.of(SubjectRecord.builder()
                        .sheetName("Sheet1")
                        .rowDataNumber(8)
                        .subjectCode("1002")
                        .subjectName("银行存款")
                        .rawValues(List.of("1002", "银行存款", "12.5"))
                        .build()))
                .build();

        ParsedValuationData standardized = service.standardize(parsedValuationData);

        assertThat(standardized.getSubjects()).hasSize(1);
        SubjectRecord subject = standardized.getSubjects().get(0);
        assertThat(subject.getStandardValues()).containsEntry("subject_cd", "1002");
        assertThat(subject.getStandardValues()).containsEntry("subject_nm", "银行存款");
        assertThat(subject.getStandardValues()).containsEntry("n_hldamt", "12.5");
        assertThat(subject.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(subject.getMappingReason()).contains("strategies");
        assertThat(subject.getMappingConfidence()).isGreaterThan(0D);
        assertThat(standardized.getHeaderMappingDecisions()).isNotEmpty();
        assertThat(standardized.getHeaderMappingDecisions().stream().allMatch(decision -> decision.getMatched() != null)).isTrue();
        assertThat(standardized.getMappingQualityReport()).isNotNull();
        assertThat(standardized.getMappingQualityReport().getHeaderTotal()).isEqualTo(3);
        assertThat(standardized.getMappingQualityReport().getHeaderMapped()).isEqualTo(3);
        assertThat(standardized.getMappingQualityReport().getSubjectTotal()).isEqualTo(1);
        assertThat(standardized.getMappingQualityReport().getSubjectMapped()).isEqualTo(1);
    }

    @Test
    void should_match_by_alias_contains_when_header_has_suffix() {
        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称"),
                        rule(3L, "n_hldamt", "数量")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称"),
                        source(3L, "n_hldamt", "数量")
                ))
        );

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .headers(List.of("科目代码", "科目名称", "持仓数量(份)"))
                .headerColumns(List.of(
                        headerColumn(0, "科目代码"),
                        headerColumn(1, "科目名称"),
                        headerColumn(2, "持仓数量(份)")
                ))
                .subjects(List.of(SubjectRecord.builder()
                        .sheetName("Sheet1")
                        .rowDataNumber(8)
                        .subjectCode("1002")
                        .subjectName("银行存款")
                        .rawValues(List.of("1002", "银行存款", "88.01"))
                        .build()))
                .build();

        ParsedValuationData standardized = service.standardize(parsedValuationData);

        assertThat(standardized.getSubjects()).hasSize(1);
        SubjectRecord subject = standardized.getSubjects().get(0);
        assertThat(subject.getStandardValues()).containsEntry("n_hldamt", "88.01");
        assertThat(subject.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(standardized.getMappingQualityReport()).isNotNull();
        assertThat(standardized.getMappingQualityReport().getHeaderMapped()).isEqualTo(3);
    }

    @Test
    void should_match_builtin_alias_when_dictionary_not_configured() {
        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称")
                ))
        );

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .headers(List.of("科目代码", "科目名称", "估值汇率(人民币)"))
                .headerColumns(List.of(
                        headerColumn(0, "科目代码"),
                        headerColumn(1, "科目名称"),
                        headerColumn(2, "估值汇率(人民币)")
                ))
                .subjects(List.of(SubjectRecord.builder()
                        .sheetName("Sheet1")
                        .rowDataNumber(8)
                        .subjectCode("1002")
                        .subjectName("银行存款")
                        .rawValues(List.of("1002", "银行存款", "7.0101"))
                        .build()))
                .build();

        ParsedValuationData standardized = service.standardize(parsedValuationData);

        assertThat(standardized.getSubjects()).hasSize(1);
        SubjectRecord subject = standardized.getSubjects().get(0);
        assertThat(subject.getStandardValues()).containsEntry("subject_cd", "1002");
        assertThat(subject.getStandardValues()).containsEntry("subject_nm", "银行存款");
        assertThat(subject.getStandardValues()).containsEntry("n_valrate", "7.0101");
        assertThat(standardized.getMappingQualityReport()).isNotNull();
        assertThat(standardized.getMappingQualityReport().getHeaderMapped()).isEqualTo(3);
    }

    @Test
    void should_match_builtin_alias_for_multilevel_local_currency_headers() {
        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称")
                ))
        );

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .headers(List.of(
                        "科目代码",
                        "科目名称",
                        "成本|本币|十亿千百十万千百十元角分",
                        "行情|本币|十亿千百十万千百十元角分",
                        "市值|本币|十亿千百十万千百十元角分",
                        "估值增值|本币|十亿千百十万千百十元角分",
                        "权益信息|本币|十亿千百十万千百十元角分"
                ))
                .headerColumns(List.of(
                        headerColumn(0, "科目代码"),
                        headerColumn(1, "科目名称"),
                        headerColumn(2, "成本|本币|十亿千百十万千百十元角分"),
                        headerColumn(3, "行情|本币|十亿千百十万千百十元角分"),
                        headerColumn(4, "市值|本币|十亿千百十万千百十元角分"),
                        headerColumn(5, "估值增值|本币|十亿千百十万千百十元角分"),
                        headerColumn(6, "权益信息|本币|十亿千百十万千百十元角分")
                ))
                .subjects(List.of(SubjectRecord.builder()
                        .sheetName("Sheet1")
                        .rowDataNumber(8)
                        .subjectCode("1002")
                        .subjectName("银行存款")
                        .rawValues(List.of("1002", "银行存款", "10.11", "12.34", "56.78", "1.23", "权益A"))
                        .build()))
                .build();

        ParsedValuationData standardized = service.standardize(parsedValuationData);

        assertThat(standardized.getSubjects()).hasSize(1);
        SubjectRecord subject = standardized.getSubjects().get(0);
        assertThat(subject.getStandardValues()).containsEntry("n_hldcst_locl", "10.11");
        assertThat(subject.getStandardValues()).containsEntry("n_valprice", "12.34");
        assertThat(subject.getStandardValues()).containsEntry("n_hldmkv_locl", "56.78");
        assertThat(subject.getStandardValues()).containsEntry("n_hldvva_l", "1.23");
        assertThat(subject.getStandardValues()).containsEntry("valuat_equity", "权益A");
        assertThat(standardized.getMappingQualityReport()).isNotNull();
        assertThat(standardized.getMappingQualityReport().getHeaderMapped()).isEqualTo(7);
    }

    @Test
    void should_match_builtin_metric_alias_when_dictionary_not_configured() {
        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称")
                ))
        );

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .headers(List.of("科目代码", "科目名称"))
                .headerColumns(List.of(
                        headerColumn(0, "科目代码"),
                        headerColumn(1, "科目名称")
                ))
                .subjects(List.of(SubjectRecord.builder()
                        .sheetName("Sheet1")
                        .rowDataNumber(8)
                        .subjectCode("1002")
                        .subjectName("银行存款")
                        .rawValues(List.of("1002", "银行存款"))
                        .build()))
                .metrics(List.of(
                        MetricRecord.builder().metricName("今日单位净值").value("1.0000").build(),
                        MetricRecord.builder().metricName("七日年化收益率").value("0.0164").build()
                ))
                .build();

        ParsedValuationData standardized = service.standardize(parsedValuationData);

        assertThat(standardized.getMetrics()).hasSize(2);
        assertThat(standardized.getMetrics().stream().allMatch(metric -> "MAPPED".equals(metric.getMappingStatus()))).isTrue();
        assertThat(standardized.getMetrics().stream().map(MetricRecord::getStandardName))
                .containsExactly("单位净值", "七日年化收益率");
        assertThat(standardized.getMappingQualityReport()).isNotNull();
        assertThat(standardized.getMappingQualityReport().getMetricMapped()).isEqualTo(2);
    }

    @Test
    void should_mark_metric_as_mapped_by_passthrough_when_no_dictionary_and_no_builtin_alias() {
        ExternalValuationStandardizationService service = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of()),
                proxySourceRepository(List.of())
        );

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .metrics(List.of(MetricRecord.builder()
                        .metricName("自定义外部指标A")
                        .value("123.45")
                        .build()))
                .build();

        ParsedValuationData standardized = service.standardize(parsedValuationData);

        assertThat(standardized.getMetrics()).hasSize(1);
        MetricRecord metric = standardized.getMetrics().get(0);
        assertThat(metric.getStandardCode()).isEqualTo("自定义外部指标A");
        assertThat(metric.getStandardName()).isEqualTo("自定义外部指标A");
        assertThat(metric.getMappingStatus()).isEqualTo("MAPPED");
        assertThat(metric.getMappingReason()).isEqualTo("Fallback passthrough metric name");
        assertThat(metric.getMappingConfidence()).isEqualTo(0.55D);
        assertThat(standardized.getMappingQualityReport()).isNotNull();
        assertThat(standardized.getMappingQualityReport().getMetricMapped()).isEqualTo(1);
    }

    private HeaderColumnMeta headerColumn(int index, String headerName) {
        return HeaderColumnMeta.builder()
                .columnIndex(index)
                .headerName(headerName)
                .headerPath(headerName)
                .build();
    }

    private FileParseRuleRepository proxyRuleRepository(List<FileParseRulePO> rows) {
        return proxyRepository(FileParseRuleRepository.class, rows);
    }

    private FileParseSourceRepository proxySourceRepository(List<FileParseSourcePO> rows) {
        return proxyRepository(FileParseSourceRepository.class, rows);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxyRepository(Class<T> repositoryType, List<?> rows) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("selectList".equals(method.getName())) {
                return rows;
            }
            if ("toString".equals(method.getName())) {
                return repositoryType.getSimpleName() + "Proxy";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
        };
        return (T) Proxy.newProxyInstance(
                repositoryType.getClassLoader(),
                new Class<?>[]{repositoryType},
                handler
        );
    }

    private FileParseRulePO rule(Long id, String columnMap, String columnMapName) {
        FileParseRulePO po = new FileParseRulePO();
        po.setId(id);
        po.setFileScene("ALL");
        po.setFileTypeName("基金资产估值表");
        po.setRegionName("column");
        po.setColumnMap(columnMap);
        po.setColumnMapName(columnMapName);
        po.setStatus(Boolean.TRUE);
        po.setMultiIndex(Boolean.FALSE);
        po.setRequired(Boolean.FALSE);
        return po;
    }

    private FileParseSourcePO source(Long id, String columnMap, String columnName) {
        FileParseSourcePO po = new FileParseSourcePO();
        po.setId(id);
        po.setFileType("COMMON");
        po.setColumnMap(columnMap);
        po.setColumnName(columnName);
        po.setStatus(Boolean.TRUE);
        return po;
    }
}
