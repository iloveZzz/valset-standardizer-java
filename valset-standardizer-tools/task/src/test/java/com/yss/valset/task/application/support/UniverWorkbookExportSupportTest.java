package com.yss.valset.task.application.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniverWorkbookExportSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UniverWorkbookExportSupport support = new UniverWorkbookExportSupport();

    @Test
    void exportKeepsDataStyleMergeFreezeAndFilter() throws Exception {
        JsonNode workbookData = objectMapper.readTree("{\n"
                + "  \"sheetOrder\": [\"sheet1\"],\n"
                + "  \"styles\": {\n"
                + "    \"header\": {\"bg\":{\"rgb\":\"#F1F3F7\"},\"cl\":{\"rgb\":\"#1F2937\"},\"bl\":1,\"ht\":2,\"vt\":2,\"tb\":2,\"bd\":{\"t\":{\"s\":1,\"cl\":{\"rgb\":\"#000000\"}},\"r\":{\"s\":1,\"cl\":{\"rgb\":\"#000000\"}},\"b\":{\"s\":1,\"cl\":{\"rgb\":\"#000000\"}},\"l\":{\"s\":1,\"cl\":{\"rgb\":\"#000000\"}}}},\n"
                + "    \"number\": {\"ht\":3,\"vt\":2,\"n\":{\"pattern\":\"0.0000\"}}\n"
                + "  },\n"
                + "  \"resources\": [{\"name\":\"SHEET_FILTER_PLUGIN\",\"data\":\"{\\\"sheet1\\\":{\\\"ref\\\":{\\\"startRow\\\":0,\\\"endRow\\\":2,\\\"startColumn\\\":0,\\\"endColumn\\\":1}}}\"}],\n"
                + "  \"sheets\": {\"sheet1\": {\n"
                + "    \"id\": \"sheet1\",\n"
                + "    \"name\": \"估值明细\",\n"
                + "    \"rowCount\": 3,\n"
                + "    \"columnCount\": 2,\n"
                + "    \"cellData\": {\n"
                + "      \"0\": {\"0\": {\"v\":\"标题\",\"s\":\"header\"}, \"1\": {\"v\":\"标题\",\"s\":\"header\"}},\n"
                + "      \"1\": {\"0\": {\"v\":\"科目\"}, \"1\": {\"v\":\"金额\",\"s\":\"header\"}},\n"
                + "      \"2\": {\"0\": {\"v\":\"银行存款\"}, \"1\": {\"v\":12.34567,\"s\":\"number\"}}\n"
                + "    },\n"
                + "    \"mergeData\": [{\"startRow\":0,\"startColumn\":0,\"endRow\":0,\"endColumn\":1}],\n"
                + "    \"columnData\": {\"0\":{\"w\":120},\"1\":{\"w\":160}},\n"
                + "    \"rowData\": {\"0\":{\"h\":34}},\n"
                + "    \"freeze\": {\"xSplit\":1,\"ySplit\":1}\n"
                + "  }}\n"
                + "}");

        byte[] content = support.export(workbookData, "估值明细");

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("估值明细");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("标题");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("银行存款");
            Cell amount = sheet.getRow(2).getCell(1);
            assertThat(amount.getNumericCellValue()).isEqualTo(12.34567);
            assertThat(amount.getCellStyle().getDataFormatString()).isEqualTo("0.0000");
            assertThat(sheet.getNumMergedRegions()).isEqualTo(1);
            assertThat(sheet.getMergedRegion(0).formatAsString()).isEqualTo("A1:B1");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(((XSSFSheet) sheet).getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(sheet.getRow(0).getHeightInPoints()).isGreaterThan(20f);
            assertThat(sheet.getColumnWidth(1)).isGreaterThan(sheet.getColumnWidth(0));
            assertThat(sheet.getRow(0).getCell(0).getCellStyle().getAlignment()).isEqualTo(HorizontalAlignment.CENTER);
        }
    }

    @Test
    void exportRejectsEmptyWorkbookData() {
        assertThatThrownBy(() -> support.export(objectMapper.nullNode(), "空表"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Univer 工作簿快照不能为空");
    }
}
