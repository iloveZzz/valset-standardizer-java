package com.yss.subjectmatch.extract.standardization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.HeaderColumnMeta;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.extract.repository.entity.FileParseRulePO;
import com.yss.subjectmatch.extract.repository.entity.FileParseSourcePO;
import com.yss.subjectmatch.extract.repository.mapper.FileParseRuleRepository;
import com.yss.subjectmatch.extract.repository.mapper.FileParseSourceRepository;
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
