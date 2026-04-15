package com.yss.subjectmatch.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.exception.FileAccessException;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.extract.extractor.CsvRawDataExtractor;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CsvRawDataExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractUtf8CsvPreservesWhitespaceAndNulls() throws Exception {
        Path file = tempDir.resolve("sample.csv");
        Files.writeString(file, "a,\"  b  \",\n1,2,3,4", StandardCharsets.UTF_8);

        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        CsvRawDataExtractor extractor = new CsvRawDataExtractor(mapper, objectMapper);

        int count = extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.CSV).sourceUri(file.toString()).build(),
                11L,
                22L
        );

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<List<ValuationFileDataPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insert(captor.capture());
        List<ValuationFileDataPO> rows = captor.getValue();
        assertThat(rows).hasSize(2);

        List<Object> firstRow = objectMapper.readValue(rows.get(0).getRowDataJson(), List.class);
        assertThat(firstRow).containsExactly("a", "  b  ", null);

        List<Object> secondRow = objectMapper.readValue(rows.get(1).getRowDataJson(), List.class);
        assertThat(secondRow).containsExactly("1", "2", "3", "4");
    }

    @Test
    void extractGbkCsvFallsBackToChineseEncoding() throws Exception {
        Path file = tempDir.resolve("sample-gbk.csv");
        byte[] bytes = "科目,余额\n资产,100".getBytes(Charset.forName("GBK"));
        Files.write(file, bytes);

        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        CsvRawDataExtractor extractor = new CsvRawDataExtractor(mapper, objectMapper);

        int count = extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.CSV).sourceUri(file.toString()).build(),
                33L,
                44L
        );

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<List<ValuationFileDataPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insert(captor.capture());
        List<ValuationFileDataPO> rows = captor.getValue();
        List<Object> firstRow = objectMapper.readValue(rows.get(0).getRowDataJson(), List.class);
        assertThat(firstRow).containsExactly("科目", "余额");
        List<Object> secondRow = objectMapper.readValue(rows.get(1).getRowDataJson(), List.class);
        assertThat(secondRow).containsExactly("资产", "100");
    }

    @Test
    void extractMissingCsvThrowsFileAccessException() {
        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        CsvRawDataExtractor extractor = new CsvRawDataExtractor(mapper, new ObjectMapper());

        assertThatThrownBy(() -> extractor.extract(
                DataSourceConfig.builder().sourceType(DataSourceType.CSV).sourceUri(tempDir.resolve("missing.csv").toString()).build(),
                1L,
                2L
        )).isInstanceOf(FileAccessException.class);

        verifyNoInteractions(mapper);
    }
}
