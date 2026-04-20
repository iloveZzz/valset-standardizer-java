package com.yss.valset.analysis.parser.file;

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

        CsvValuationDataParser parser = new CsvValuationDataParser(new ObjectMapper());
        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getSheetName()).isEqualTo("CSV_RAW_DATA");
        assertThat(parsed.getTitle()).isEqualTo("中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表");
        assertThat(parsed.getHeaderRowNumber()).isEqualTo(4);
        assertThat(parsed.getDataStartRowNumber()).isEqualTo(5);
        assertThat(parsed.getBasicInfo()).containsEntry("估值日期", "2025-03-05");
        assertThat(parsed.getBasicInfo()).containsEntry("单位净值", "1.0102");
        assertThat(parsed.getBasicInfo()).containsEntry("单位", "元");
        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("1002");
        assertThat(parsed.getMetrics()).hasSize(2);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("基金资产净值");
        assertThat(parsed.getMetrics().get(1).getMetricName()).isEqualTo("基金单位净值");
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

        CsvValuationDataParser parser = new CsvValuationDataParser(new ObjectMapper());
        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("100201");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("单位净值");
    }

    @Test
    void parseMetricLikeRowsWithAmountValuesShouldNotEnterSubjects() throws Exception {
        Path csv = tempDir.resolve("sample-metric-like.csv");
        Files.writeString(csv, String.join("\n",
                "证券投资基金估值表",
                "实收资本相关测试表",
                ",估值日期：2025-03-05,,,,,单位净值：,,,1.0102,,单位：元,",
                ",科目代码,科目名称,币种,数量,单位成本,成本,成本占净值%,行情,市值,市值占净值%,估值增值,停牌信息,债券每百元利息",
                ",证券投资合计:, ,434689297.91,1,434689297.91,98.99,,434689297.91,98.99,,,,",
                ",实收资本金额,,434689297.91,1,434689297.91,98.99,,434689297.91,98.99,,,,",
                ",实收资本,,434689297.91,,434689297.91,98.99,,434689297.91,98.99,,,,"
        ), StandardCharsets.UTF_8);

        CsvValuationDataParser parser = new CsvValuationDataParser(new ObjectMapper());
        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getSubjects()).isEmpty();
        assertThat(parsed.getMetrics()).hasSize(3);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("证券投资合计");
        assertThat(parsed.getMetrics().get(1).getMetricName()).isEqualTo("实收资本金额");
        assertThat(parsed.getMetrics().get(2).getMetricName()).isEqualTo("实收资本");
    }

    @Test
    void parseSubjectNameContainingDigitsShouldStillEnterSubjects() throws Exception {
        Path csv = tempDir.resolve("sample-subject-digits.csv");
        Files.writeString(csv, String.join("\n",
                "证券投资基金估值表",
                "中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表",
                ",估值日期：2025-03-05,,,,,单位净值：,,,1.0102,,单位：元,",
                ",科目代码,科目名称,币种,数量,单位成本,成本,成本占净值%,行情,市值,市值占净值%,估值增值,停牌信息,债券每百元利息",
                ",1101.02.14.01.241068,24鄂旅03,,,131788.22,0.03,,131788.22,0.03,,,,",
                ",实收资本金额,,434689297.91,1,434689297.91,98.99,,434689297.91,98.99,,,,"
        ), StandardCharsets.UTF_8);

        CsvValuationDataParser parser = new CsvValuationDataParser(new ObjectMapper());
        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("1101021401241068");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("24鄂旅03");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("实收资本金额");
    }

    @Test
    void parseSubjectCodeShouldNormalizeSpacedAndMixedAlphanumericFormats() throws Exception {
        Path csv = tempDir.resolve("sample-subject-code-normalize.csv");
        Files.writeString(csv, String.join("\n",
                "证券投资基金估值表",
                "科目代码归一化测试",
                ",估值日期：2025-03-05",
                ",,科目代码,科目名称,币种,数量",
                ",,2221 06 01,应付申购款,100.00",
                ",,1103.B9.01.2020016 IB,20江苏银行永续债,200.00"
        ), StandardCharsets.UTF_8);

        CsvValuationDataParser parser = new CsvValuationDataParser(new ObjectMapper());
        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.CSV)
                .sourceUri(csv.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(2);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("22210601");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("应付申购款");
        assertThat(parsed.getSubjects().get(1).getSubjectCode()).isEqualTo("1103B9012020016IB");
        assertThat(parsed.getSubjects().get(1).getSubjectName()).isEqualTo("20江苏银行永续债");
    }
}
