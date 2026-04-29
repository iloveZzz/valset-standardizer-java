package com.yss.valset.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.exception.FileAccessException;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.extract.extractor.PoiRawDataExtractor;
import com.yss.valset.extract.repository.entity.ValuationFileDataPO;
import com.yss.valset.extract.repository.entity.ValuationSheetStylePO;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PoiRawDataExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractXlsxSerializesFormulaDateAndNullCells() throws Exception {
        Path file = tempDir.resolve("sample.xlsx");
        createXlsx(file);

        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);

        ObjectMapper objectMapper = new ObjectMapper();
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, objectMapper,sheetStyleMapper);

        int count = extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.EXCEL).sourceUri(file.toString()).build(),
                100L,
                200L
        );

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<List<ValuationFileDataPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insert(captor.capture(), eq(1000));
        List<ValuationFileDataPO> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        List<Object> values = objectMapper.readValue(rows.get(0).getRowDataJson(), List.class);
        assertThat(values).containsExactly("2023-03-21", "1", "3", null);
        assertThat(rows.get(0).getRowDataNumber()).isEqualTo(1);
    }

    @Test
    void extractXlsxCanSkipStyleSnapshot() throws Exception {
        Path file = tempDir.resolve("sample-skip-style.xlsx");
        createXlsx(file);

        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        doReturn(List.of()).when(mapper).insert(anyList(), anyInt());

        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, new ObjectMapper(),sheetStyleMapper);
        ReflectionTestUtils.setField(extractor, "skipExcelStyleParsing", true);

        int count = extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.EXCEL).sourceUri(file.toString()).build(),
                102L,
                202L
        );

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<List<ValuationFileDataPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insert(captor.capture(), eq(1000));
        List<ValuationFileDataPO> rows = captor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getRowDataJson()).isNotBlank();
    }

    @Test
    void extractXlsReadsLegacyWorkbook() throws Exception {
        Path file = tempDir.resolve("sample.xls");
        createXls(file);

        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, new ObjectMapper(),sheetStyleMapper);

        int count = extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.EXCEL).sourceUri(file.toString()).build(),
                101L,
                201L
        );

        assertThat(count).isEqualTo(1);
        verify(mapper).insert(anyList(), eq(1000));
    }

    @Test
    void extractSpreadsheetXmlWorkbookReadsXmlWorkbook() throws Exception {
        Path file = tempDir.resolve("sample-spreadsheetml.xls");
        createSpreadsheetXml(file);

        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, new ObjectMapper(),sheetStyleMapper);

        int count = extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.EXCEL).sourceUri(file.toString()).build(),
                103L,
                203L
        );

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<List<ValuationFileDataPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insert(captor.capture(), eq(1000));
        List<ValuationFileDataPO> rows = captor.getValue();
        assertThat(rows).hasSize(2);
        List<Object> firstRow = new ObjectMapper().readValue(rows.get(0).getRowDataJson(), List.class);
        assertThat(firstRow).containsExactly("姓名", "年龄");
        List<Object> secondRow = new ObjectMapper().readValue(rows.get(1).getRowDataJson(), List.class);
        assertThat(secondRow).containsExactly("张三", "18");
        verify(sheetStyleMapper).insert(any(ValuationSheetStylePO.class));
    }

    @Test
    void extractMissingWorkbookThrowsFileAccessException() {
        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, new ObjectMapper(),sheetStyleMapper);
        assertThatThrownBy(() -> extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.EXCEL).sourceUri(tempDir.resolve("missing.xlsx").toString()).build(),
                1L,
                2L
        )).isInstanceOf(FileAccessException.class);

        verifyNoInteractions(mapper);
    }

    private void createXlsx(Path file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);

            Cell dateCell = row.createCell(0);
            CellStyle dateStyle = workbook.createCellStyle();
            short format = workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd");
            dateStyle.setDataFormat(format);
            dateCell.setCellStyle(dateStyle);
            Date date = Date.from(LocalDate.of(2023, 3, 21).atStartOfDay(ZoneId.systemDefault()).toInstant());
            dateCell.setCellValue(date);

            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(1);
            Cell formulaCell = row.createCell(2);
            formulaCell.setCellFormula("B1*3");
            row.createCell(3, CellType.BLANK);

            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            try (OutputStream outputStream = Files.newOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    private void createXls(Path file) throws Exception {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("legacy");
            try (OutputStream outputStream = Files.newOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    private void createSpreadsheetXml(Path file) throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <?mso-application progid="Excel.Sheet"?>
                <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                          xmlns:o="urn:schemas-microsoft-com:office:office"
                          xmlns:x="urn:schemas-microsoft-com:office:excel"
                          xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                  <Worksheet ss:Name="Sheet1">
                    <Table>
                      <Row>
                        <Cell><Data ss:Type="String">姓名</Data></Cell>
                        <Cell><Data ss:Type="String">年龄</Data></Cell>
                      </Row>
                      <Row>
                        <Cell><Data ss:Type="String">张三</Data></Cell>
                        <Cell><Data ss:Type="Number">18</Data></Cell>
                      </Row>
                    </Table>
                  </Worksheet>
                </Workbook>
                """;
        Files.writeString(file, xml);
    }
}
