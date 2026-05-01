package com.yss.valset.extract.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.ParsedValuationData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OdsValuationDataParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parseWorkbookLikeRowsWithTitleBasicInfoHeaderSubjectsAndMetrics() throws Exception {
        Path workbookPath = writeWorkbook("sample.xlsx", List.of(
                row("证券投资基金估值表"),
                row("中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表"),
                row("", "估值日期：2025-03-05", "", "", "", "", "单位净值：", "", "", "1.0102", "", "单位：元", ""),
                row("", "科目代码", "科目名称", "币种", "数量", "单位成本", "成本", "成本占净值%", "行情", "市值", "市值占净值%", "估值增值", "停牌信息", "债券每百元利息"),
                row("-", "1002", "银行存款", "", "", "196926.51", "0.0448", "", "196926.51", "0.0448", "", "", ""),
                row("", "基金资产净值:", "", "", "", "439134701.52", "100", "", "439134701.52", "100", "", "", ""),
                row("", "基金单位净值:", "1.0102", "", "", "", "", "", "", "", "", "", ""),
                row("", "制表：", "王雪洁", "复核：", "", "刘娟", "", "", "", "", "", "", "")
        ));

        ParsedValuationData parsed = new OdsValuationDataParser(new ObjectMapper()).parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri(workbookPath.toString())
                .build());

        assertThat(parsed.getWorkbookPath()).isEqualTo(workbookPath.toString());
        assertThat(parsed.getSheetName()).isEqualTo("ODS_RAW_DATA");
        assertThat(parsed.getTitle()).isEqualTo("中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表");
        assertThat(parsed.getHeaderRowNumber()).isEqualTo(4);
        assertThat(parsed.getDataStartRowNumber()).isEqualTo(5);
        assertThat(parsed.getBasicInfo()).containsEntry("估值日期", "2025-03-05");
        assertThat(parsed.getBasicInfo()).containsEntry("单位净值", "1.0102");
        assertThat(parsed.getBasicInfo()).containsEntry("单位", "元");
        assertThat(parsed.getHeaders()).contains("科目代码", "科目名称", "币种", "数量", "单位成本", "成本", "市值");
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        HeaderColumnMeta firstColumn = parsed.getHeaderColumns().get(0);
        assertThat(firstColumn.getColumnIndex()).isEqualTo(0);
        assertThat(firstColumn.getHeaderName()).isEqualTo("");
        assertThat(firstColumn.getHeaderPath()).isEqualTo("");
        assertThat(firstColumn.getPathSegments()).isEmpty();
        assertThat(firstColumn.getBlankColumn()).isTrue();
        assertThat(parsed.getHeaderColumns().stream().anyMatch(column -> "成本".equals(column.getHeaderPath()))).isTrue();
        assertThat(parsed.getSubjects()).isNotEmpty();
        assertThat(parsed.getSubjects().stream().anyMatch(subject -> "1002".equals(subject.getSubjectCode()))).isTrue();
        assertThat(parsed.getSubjects().stream().anyMatch(subject -> subject.getRawValues() != null && !subject.getRawValues().isEmpty())).isTrue();
    }

    @Test
    void parseFromRowsWithLeadingPlaceholdersBeforeSubjectCode() throws Exception {
        Path workbookPath = writeWorkbook("sample-leading-placeholders.xlsx", List.of(
                row("XX001测试组合估值表"),
                row("估值日期：2025-03-05"),
                row("", ""),
                row("", "", "科目代码", "科目名称", "币种", "数量"),
                row("", "-", "1002.01", "银行存款", "人民币", "100.00"),
                row("单位净值", "1.0102")
        ));

        ParsedValuationData parsed = new OdsValuationDataParser(new ObjectMapper()).parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri(workbookPath.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("100201");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("银行存款");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("单位净值");
        assertThat(parsed.getMetrics().get(0).getValue()).isEqualTo("1.0102");
    }

    @Test
    void parseFromRowsWithSubjectCodeInLaterColumn() throws Exception {
        Path workbookPath = writeWorkbook("sample-later-column.xlsx", List.of(
                row("XX002延后科目代码测试"),
                row("", ""),
                row("", "", "", "科目代码", "科目名称", "币种", "数量"),
                row("", "-", "", "1101.02.14", "上交所企业债", "200000"),
                row("单位净值", "0.9988")
        ));

        ParsedValuationData parsed = new OdsValuationDataParser(new ObjectMapper()).parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri(workbookPath.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("11010214");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("上交所企业债");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("单位净值");
        assertThat(parsed.getMetrics().get(0).getValue()).isEqualTo("0.9988");
    }

    @Test
    void parseSubjectCodeShouldNormalizeSpacedAndMixedAlphanumericFormats() throws Exception {
        Path workbookPath = writeWorkbook("sample-subject-code-normalize.xlsx", List.of(
                row("证券投资基金估值表"),
                row("科目代码归一化测试"),
                row("", "估值日期：2025-03-05"),
                row("", "", "科目代码", "科目名称", "币种", "数量"),
                row("", "", "2221 06 01", "应付申购款", "", "100.00"),
                row("", "", "1103.B9.01.2020016 IB", "20江苏银行永续债", "", "200.00")
        ));

        ParsedValuationData parsed = new OdsValuationDataParser(new ObjectMapper()).parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri(workbookPath.toString())
                .build());

        assertThat(parsed.getSubjects()).hasSize(2);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("22210601");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("应付申购款");
        assertThat(parsed.getSubjects().get(1).getSubjectCode()).isEqualTo("1103B9012020016IB");
        assertThat(parsed.getSubjects().get(1).getSubjectName()).isEqualTo("20江苏银行永续债");
    }

    private Path writeWorkbook(String fileName, List<List<String>> rows) throws Exception {
        Path workbookPath = tempDir.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<String> rowValues = rows.get(rowIndex);
                Row row = sheet.createRow(rowIndex);
                for (int columnIndex = 0; columnIndex < rowValues.size(); columnIndex++) {
                    String value = rowValues.get(columnIndex);
                    if (value == null) {
                        continue;
                    }
                    row.createCell(columnIndex).setCellValue(value);
                }
            }
            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }
        return workbookPath;
    }

    private List<String> row(String... values) {
        return List.of(values);
    }
}
