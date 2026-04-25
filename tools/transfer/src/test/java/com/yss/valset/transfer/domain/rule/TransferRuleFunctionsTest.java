package com.yss.valset.transfer.domain.rule;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.yss.valset.transfer.domain.form.TransferTagFormTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferRuleFunctionsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectExcelAndCsvFiles() {
        TransferRuleFunctions functions = new TransferRuleFunctions();

        assertThat(functions.isExcel("demo.xlsx")).isTrue();
        assertThat(functions.isExcelFile("demo.xls")).isTrue();
        assertThat(functions.isCsv("demo.csv")).isTrue();
        assertThat(functions.isCsvFile("demo.txt")).isFalse();
    }

    @Test
    void shouldReadExcelDataAsNestedStringList() throws IOException {
        Path excelFile = tempDir.resolve("sample.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("年龄");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("张三");
            row.createCell(1).setCellValue(18);

            try (OutputStream outputStream = Files.newOutputStream(excelFile)) {
                workbook.write(outputStream);
            }
        }

        TransferRuleFunctions functions = new TransferRuleFunctions();
        List<List<String>> data = functions.readExcelData(excelFile);

        assertThat(data).containsExactly(
                List.of("姓名", "年龄"),
                List.of("张三", "18")
        );
    }

    @Test
    void shouldReadCsvDataAsNestedStringList() throws IOException {
        Path csvFile = tempDir.resolve("sample.csv");
        Files.writeString(csvFile, "姓名,年龄\n张三,18\n李四,\n", StandardCharsets.UTF_8);

        TransferRuleFunctions functions = new TransferRuleFunctions();
        List<List<String>> data = functions.readCsvData(csvFile);

        assertThat(data).containsExactly(
                List.of("姓名", "年龄"),
                List.of("张三", "18"),
                Arrays.asList("李四", null)
        );
    }

    @Test
    void shouldExposeNewFunctionsToQlexpress() throws Exception {
        ScriptRuleEngineAdapter engine = new ScriptRuleEngineAdapter();

        Path excelFile = tempDir.resolve("rule.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("A");
            try (OutputStream outputStream = Files.newOutputStream(excelFile)) {
                workbook.write(outputStream);
            }
        }

        Object excelResult = engine.evaluateExpression("readExcelData(filePath)", Map.of(
                "filePath", excelFile.toString()
        ));
        assertThat(excelResult).isInstanceOf(List.class);
        assertThat((List<?>) excelResult).hasSize(1);

        Path csvFile = tempDir.resolve("rule.csv");
        Files.writeString(csvFile, "A,B\n1,2\n", StandardCharsets.UTF_8);

        Object csvResult = engine.evaluateExpression("readCsvData(filePath)", Map.of(
                "filePath", csvFile.toString()
        ));
        assertThat(csvResult).isInstanceOf(List.class);
        assertThat((List<?>) csvResult).hasSize(2);

        assertThat(engine.evaluateBooleanExpression(
                "isExcelFile(fileName) && isCsvFile(csvName)",
                Map.of("fileName", "demo.xlsx", "csvName", "demo.csv")
        )).isTrue();
        assertThat(engine.evaluateBooleanExpression(
                "isExcel(fileName) && isCsv(csvName)",
                Map.of("fileName", "demo.xlsx", "csvName", "demo.csv")
        )).isTrue();
    }

    @Test
    void shouldEvaluateDefaultTagScriptWithoutTrim() throws Exception {
        ScriptRuleEngineAdapter engine = new ScriptRuleEngineAdapter();
        TransferTagFormTemplate template = new TransferTagFormTemplate();
        String scriptBody = String.valueOf(template.initialValues().get("scriptBody"));

        assertThat(scriptBody).doesNotContain("trim()");

        Path excelFile = tempDir.resolve("valuation.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("科目代码");
            header.createCell(1).setCellValue("科目名称");
            try (OutputStream outputStream = Files.newOutputStream(excelFile)) {
                workbook.write(outputStream);
            }
        }

        assertThat(engine.evaluateBooleanExpression(scriptBody, Map.of(
                "filePath", "   ",
                "path", excelFile.toString(),
                "tagMeta", Map.of("scanLimit", 10, "headerKeywords", List.of("科目代码", "科目名称"))
        ))).isTrue();
    }
}
