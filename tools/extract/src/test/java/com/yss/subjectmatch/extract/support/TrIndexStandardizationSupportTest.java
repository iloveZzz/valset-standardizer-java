package com.yss.subjectmatch.extract.support;

import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.repository.entity.TrIndexPO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrIndexStandardizationSupportTest {

    @Test
    void should_build_index_rows_from_standardized_metrics() {
        ParsedValuationData parsedValuationData = ParsedValuationData.builder()
                .title("20230321基金资产估值表")
                .basicInfo(Map.of("biz_date", "2023-03-21"))
                .metrics(List.of(
                        MetricRecord.builder()
                                .sheetName("Sheet1")
                                .rowDataNumber(8)
                                .metricName("市值")
                                .value("123.45")
                                .standardValues(Map.of("metric_name", "市值", "metric_value", "123.45"))
                                .build()
                ))
                .build();

        List<TrIndexPO> rows = TrIndexStandardizationSupport.buildRows(parsedValuationData, "CSV", "20230321基金资产估值表.xlsx");

        assertThat(rows).hasSize(1);
        TrIndexPO row = rows.get(0);
        assertThat(row.getBizDate()).isEqualTo("20230321");
        assertThat(row.getIndxNm()).isEqualTo("市值");
        assertThat(row.getIndxValu()).isEqualTo("123.45");
        assertThat(row.getSourceTp()).isEqualTo("CSV");
        assertThat(row.getSourceSign()).isEqualTo("20230321基金资产估值表.xlsx");
    }
}
