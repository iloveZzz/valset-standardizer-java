package com.yss.valset.extract.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CsvValuationDataParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parseWorkbookLikeCsvWithTitleBasicInfoHeaderSubjectsAndMetrics() throws Exception {
        Path csv = tempDir.resolve("sample.csv");
        Files.writeString(csv, String.join("\n",
                "证券投资基金估值表",
                "中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表",
                ",估值日期：2025-03-05,,,,,单位净值：,,,1.0102,,单位：元,",
                ",科目代码,科目名称,币种,数量,单位成本,成本,成本占净值%,行情,市值,市值占净值%,估值增值,停牌信息,债券每百元利息",
                "-,1002,银行存款,,,196926.51,0.0448,,196926.51,0.0448,,,,",
                ",基金资产净值:, , , ,439134701.52,100,,439134701.52,100,,,,",
                ",基金单位净值:,1.0102,,,,,,,,,,,",
                ",制表：,王雪洁,复核：,,刘娟,,,,,,,,"
        ), StandardCharsets.UTF_8);

        ParsedValuationData parsed = new CsvValuationDataParser(new ObjectMapper()).parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getWorkbookPath()).isEqualTo(csv.toAbsolutePath().toString());
        assertThat(parsed.getSheetName()).isEqualTo("CSV_RAW_DATA");
        assertThat(parsed.getTitle()).isEqualTo("中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表");
        assertThat(parsed.getHeaderRowNumber()).isEqualTo(4);
        assertThat(parsed.getDataStartRowNumber()).isEqualTo(5);
        assertThat(parsed.getBasicInfo()).containsEntry("估值日期", "2025-03-05");
        assertThat(parsed.getBasicInfo()).containsEntry("单位净值", "1.0102");
        assertThat(parsed.getBasicInfo()).containsEntry("单位", "元");
        assertThat(parsed.getHeaders()).contains("科目代码", "科目名称", "币种", "数量", "单位成本", "成本", "市值");
        assertThat(parsed.getSubjects()).isNotEmpty();
        assertThat(parsed.getSubjects().stream().anyMatch(subject -> "1002".equals(subject.getSubjectCode()))).isTrue();
    }

    @Test
    void parseCsvWithLeadingPlaceholderBeforeSubjectCode() throws Exception {
        Path csv = tempDir.resolve("sample-leading.csv");
        Files.writeString(csv, String.join("\n",
                "XX001测试组合估值表",
                "估值日期：2025-03-05",
                ",,科目代码,科目名称,币种,数量",
                ",-,1002.01,银行存款,人民币,100.00",
                "单位净值,1.0102"
        ), StandardCharsets.UTF_8);

        ParsedValuationData parsed = new CsvValuationDataParser(new ObjectMapper()).parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("100201");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("银行存款");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("单位净值");
    }
}
