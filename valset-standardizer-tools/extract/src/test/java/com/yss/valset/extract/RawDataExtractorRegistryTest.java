package com.yss.valset.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.exception.UnsupportedDataSourceException;
import com.yss.valset.extract.domain.extractor.RawDataExtractor;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.extract.extractor.CsvRawDataExtractor;
import com.yss.valset.extract.extractor.PoiRawDataExtractor;
import com.yss.valset.extract.extractor.RawDataExtractorRegistry;
import com.yss.valset.application.service.workflow.WorkflowRuntimeParamService;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RawDataExtractorRegistryTest {

    @Test
    void dispatchesExcelAndCsvExtractors() {
        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        WorkflowRuntimeParamService workflowRuntimeParamService = mock(WorkflowRuntimeParamService.class);
        when(workflowRuntimeParamService.skipExcelStyleParsing()).thenReturn(false);
        PoiRawDataExtractor poi = new PoiRawDataExtractor(mapper, new ObjectMapper(), sheetStyleMapper, workflowRuntimeParamService);
        CsvRawDataExtractor csv = new CsvRawDataExtractor(mapper, objectMapper);
        RawDataExtractorRegistry registry = new RawDataExtractorRegistry(poi, csv);

        RawDataExtractor excelExtractor = registry.getExtractor(DataSourceType.EXCEL);
        RawDataExtractor csvExtractor = registry.getExtractor(DataSourceType.CSV);

        assertThat(excelExtractor).isSameAs(poi);
        assertThat(csvExtractor).isSameAs(csv);
    }

    @Test
    void rejectsUnsupportedTypes() {
        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        WorkflowRuntimeParamService workflowRuntimeParamService = mock(WorkflowRuntimeParamService.class);
        when(workflowRuntimeParamService.skipExcelStyleParsing()).thenReturn(false);
        RawDataExtractorRegistry registry = new RawDataExtractorRegistry(
                new PoiRawDataExtractor(mapper, new ObjectMapper(), sheetStyleMapper, workflowRuntimeParamService),
                new CsvRawDataExtractor(mapper, objectMapper)
        );

        assertThatThrownBy(() -> registry.getExtractor(DataSourceType.API))
                .isInstanceOf(UnsupportedDataSourceException.class);
    }
}
