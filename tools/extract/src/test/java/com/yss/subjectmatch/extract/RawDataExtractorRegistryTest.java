package com.yss.subjectmatch.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.exception.UnsupportedDataSourceException;
import com.yss.subjectmatch.domain.extractor.RawDataExtractor;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.extract.extractor.CsvRawDataExtractor;
import com.yss.subjectmatch.extract.extractor.PoiRawDataExtractor;
import com.yss.subjectmatch.extract.extractor.RawDataExtractorRegistry;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.subjectmatch.extract.repository.mapper.ValuationSheetStyleMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RawDataExtractorRegistryTest {

    @Test
    void dispatchesExcelAndCsvExtractors() {
        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ValuationSheetStyleMapper sheetStyleMapper = mock(ValuationSheetStyleMapper.class);
        PoiRawDataExtractor poi = new PoiRawDataExtractor(mapper, new ObjectMapper(),sheetStyleMapper);
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
        RawDataExtractorRegistry registry = new RawDataExtractorRegistry(
                new PoiRawDataExtractor(mapper, new ObjectMapper(),sheetStyleMapper),
                new CsvRawDataExtractor(mapper, objectMapper)
        );

        assertThatThrownBy(() -> registry.getExtractor(DataSourceType.API))
                .isInstanceOf(UnsupportedDataSourceException.class);
    }
}
