package com.yss.subjectmatch.extract.support;

import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.extract.repository.entity.TrDwdJjhzgzbPO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JjhzgzbStandardizationSupportTest {

    @Test
    void should_map_standard_fields_to_jjhzgzb_row() {
        Map<String, Object> standardValues = new LinkedHashMap<>();
        standardValues.put("org_cd", "ORG-1");
        standardValues.put("pd_cd", "PD-1");
        standardValues.put("biz_date", "2023-03-21");
        standardValues.put("subject_cd", "1002");
        standardValues.put("subject_nm", "银行存款");
        standardValues.put("ccy_cd", "CNY");
        standardValues.put("n_valrate", BigDecimal.ONE);
        standardValues.put("n_hldamt", new BigDecimal("12.5"));
        standardValues.put("n_price_cost", new BigDecimal("8.1"));
        standardValues.put("n_hldcst", new BigDecimal("8107579260.48"));
        standardValues.put("n_cb_jz_bl", "93.6036%");
        standardValues.put("n_valprice", new BigDecimal("1.02"));
        standardValues.put("n_hldmkv", new BigDecimal("8107579260.48"));
        standardValues.put("n_sz_jz_bl", "93.6036%");
        standardValues.put("n_hldvva", BigDecimal.ZERO);
        standardValues.put("susp_info", "N");
        standardValues.put("valuat_equity", "权益");
        standardValues.put("source_tp", "CSV");
        standardValues.put("source_sign", "existing");

        SubjectRecord subject = SubjectRecord.builder()
                .sheetName("Sheet1")
                .rowDataNumber(8)
                .subjectCode("1002")
                .subjectName("银行存款")
                .standardValues(standardValues)
                .build();

        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .basicInfo(Map.of("日期", "2023-03-21"))
                .subjects(List.of(subject))
                .build();

        List<TrDwdJjhzgzbPO> rows = JjhzgzbStandardizationSupport.buildRows(parsedValuationData, "CSV");

        assertThat(rows).hasSize(1);
        TrDwdJjhzgzbPO row = rows.get(0);
        assertThat(row.getOrgCd()).isEqualTo("ORG-1");
        assertThat(row.getPdCd()).isEqualTo("PD-1");
        assertThat(row.getBizDate()).isEqualTo("20230321");
        assertThat(row.getSubjectCd()).isEqualTo("1002");
        assertThat(row.getSubjectNm()).isEqualTo("银行存款");
        assertThat(row.getNHldamt()).isEqualTo(new BigDecimal("12.5"));
        assertThat(row.getNCbJzBl()).isEqualTo(new BigDecimal("0.936036"));
        assertThat(row.getNSzJzBl()).isEqualTo(new BigDecimal("0.936036"));
        assertThat(row.getSourceTp()).isEqualTo("CSV");
        assertThat(row.getSn()).isEqualTo(8);
        assertThat(row.getDataDt()).isEqualTo("20230321");
    }
}
