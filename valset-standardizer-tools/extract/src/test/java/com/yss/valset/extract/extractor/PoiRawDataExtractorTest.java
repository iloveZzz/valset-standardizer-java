package com.yss.valset.extract.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.service.workflow.WorkflowRuntimeParamService;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.extract.repository.entity.ValuationFileDataPO;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoiRawDataExtractorTest {

    @Test
    void shouldPersistPhysicalExcelRowNumberInsteadOfDenseSequence() throws Exception {
        Path workbook = createWorkbookWithBlankRowsBeforeData();
        ValuationFileDataMapper fileDataMapper = mock(ValuationFileDataMapper.class);
        WorkflowRuntimeParamService runtimeParamService = mock(WorkflowRuntimeParamService.class);
        when(runtimeParamService.skipExcelStyleParsing()).thenReturn(true);
        PoiRawDataExtractor extractor = new PoiRawDataExtractor(
                fileDataMapper,
                new ObjectMapper(),
                null,
                runtimeParamService);

        int rowCount = extractor.extract(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri(workbook.toString())
                .build(), 100L, 200L);

        ArgumentCaptor<List<ValuationFileDataPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileDataMapper).insert(captor.capture(), anyInt());
        List<ValuationFileDataPO> rows = captor.getValue();
        assertThat(rowCount).isEqualTo(3);
        assertThat(rows).extracting(ValuationFileDataPO::getRowDataNumber)
                .containsExactly(2, 4, 7);
    }

    private static Path createWorkbookWithBlankRowsBeforeData() throws Exception {
        Path path = Files.createTempFile("valuation-ods-row-number-", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("估值表");
            createRow(sheet, 1, "人民币理财产品估值表");
            createRow(sheet, 3, "估值日期：", "2025年05月14日");
            createRow(sheet, 6, "科目代码", "科目名称", "市值");
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            }
        }
        return path;
    }

    private static void createRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            row.createCell(columnIndex).setCellValue(values[columnIndex]);
        }
    }
}
