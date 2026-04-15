package com.yss.subjectmatch.extract.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.yss.subjectmatch.domain.exception.FileAccessException;
import com.yss.subjectmatch.domain.extractor.RawDataExtractor;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Apache Commons CSV 原始数据提取器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvRawDataExtractor implements RawDataExtractor {

    private static final int BATCH_SIZE = 1000;
    private static final Charset[] CANDIDATE_CHARSETS = new Charset[] {
            Charset.forName("UTF-8"),
            Charset.forName("GBK"),
            Charset.forName("GB2312")
    };

    private final ValuationFileDataMapper valuationFileDataMapper;
    private final ObjectMapper objectMapper;

    @Override
    public int extract(DataSourceConfig config, Long taskId, Long fileId) {
        if (taskId == null || fileId == null) {
            throw new IllegalArgumentException("taskId and fileId must not be null");
        }

        Path filePath = Paths.get(config.getSourceUri());
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileAccessException(config.getSourceUri());
        }

        log.info("开始提取 CSV 原始数据，filePath={}, taskId={}, fileId={}", filePath, taskId, fileId);
        IOException lastError = null;
        for (Charset charset : CANDIDATE_CHARSETS) {
            try {
                int rowCount = extractWithCharset(filePath, charset, taskId, fileId);
                log.info("CSV 原始数据提取完成，filePath={}, charset={}, rowCount={}", filePath, charset, rowCount);
                return rowCount;
            } catch (CharacterCodingException e) {
                lastError = e;
                log.debug("CSV 文件 {} 使用字符集 {} 解码失败，尝试下一个字符集", filePath, charset, e);
            } catch (IOException e) {
                lastError = e;
                if (isLikelyEncodingFailure(e)) {
                    log.debug("CSV 文件 {} 使用字符集 {} 读取失败，尝试下一个字符集", filePath, charset, e);
                    continue;
                }
                throw new FileAccessException(config.getSourceUri(), e);
            }
        }

        throw new FileAccessException(config.getSourceUri(), lastError);
    }

    @Override
    public DataSourceType supportedType() {
        return DataSourceType.CSV;
    }

    private int extractWithCharset(Path filePath, Charset charset, Long taskId, Long fileId) throws IOException {
        int persistedRows = 0;
        int rowDataNumber = 0;
        List<ValuationFileDataPO> batch = new ArrayList<>(BATCH_SIZE);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(false)
                .setTrim(false)
                .build();

        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset.newDecoder()
                     .onMalformedInput(CodingErrorAction.REPORT)
                     .onUnmappableCharacter(CodingErrorAction.REPORT)));
             CSVParser parser = new CSVParser(reader, format)) {

            try {
                for (CSVRecord record : parser) {
                    rowDataNumber++;
                    List<String> values = extractValues(record);
                    String rowDataJson = serializeRecord(values);
                    if (rowDataJson == null) {
                        log.warn("CSV 第 {} 行序列化失败，已跳过", rowDataNumber);
                        continue;
                    }
                    ValuationFileDataPO po = new ValuationFileDataPO();
                    po.setId(IdWorker.getId());
                    po.setTaskId(taskId);
                    po.setFileId(fileId);
                    po.setRowDataNumber(rowDataNumber);
                    po.setRowDataJson(rowDataJson);
                    batch.add(po);
                    persistedRows++;

                    if (batch.size() >= BATCH_SIZE) {
                        valuationFileDataMapper.insert(batch, BATCH_SIZE);
                        batch.clear();
                    }
                }
            } catch (UncheckedIOException e) {
                throw new IOException(e.getCause());
            }
        }

        if (!batch.isEmpty()) {
            valuationFileDataMapper.insert(batch, BATCH_SIZE);
        }

        return persistedRows;
    }

    private List<String> extractValues(CSVRecord record) {
        List<String> values = new ArrayList<>(record.size());
        for (int index = 0; index < record.size(); index++) {
            String value = record.get(index);
            values.add(value == null || value.isEmpty() ? null : value);
        }
        return values;
    }

    private String serializeRecord(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.error("CSV 记录序列化为 JSON 失败", e);
            return null;
        }
    }

    private boolean isLikelyEncodingFailure(IOException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("MalformedInput") || message.contains("UnmappableCharacter"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
