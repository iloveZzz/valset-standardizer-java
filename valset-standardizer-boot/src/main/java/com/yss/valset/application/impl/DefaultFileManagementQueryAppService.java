package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.dto.ValsetFileIngestLogViewDTO;
import com.yss.valset.application.dto.ValsetFileInfoViewDTO;
import com.yss.valset.application.dto.ValuationSheetStyleViewDTO;
import com.yss.valset.application.service.FileManagementQueryAppService;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.gateway.ValsetFileIngestLogGateway;
import com.yss.valset.domain.model.ValsetFileIngestLog;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.extract.repository.entity.ValuationSheetStylePO;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 文件管理查询服务默认实现。
 */
@Slf4j
@Service
public class DefaultFileManagementQueryAppService implements FileManagementQueryAppService {

    private static final int DEFAULT_LIMIT = 50;

    private final ValsetFileInfoGateway subjectMatchFileInfoGateway;
    private final ValsetFileIngestLogGateway subjectMatchFileIngestLogGateway;
    private final ValuationSheetStyleMapper valuationSheetStyleMapper;
    private final ObjectMapper objectMapper;

    public DefaultFileManagementQueryAppService(ValsetFileInfoGateway subjectMatchFileInfoGateway,
                                               ValsetFileIngestLogGateway subjectMatchFileIngestLogGateway,
                                               ValuationSheetStyleMapper valuationSheetStyleMapper,
                                               ObjectMapper objectMapper) {
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.subjectMatchFileIngestLogGateway = subjectMatchFileIngestLogGateway;
        this.valuationSheetStyleMapper = valuationSheetStyleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValsetFileInfoViewDTO queryFileInfo(Long fileId) {
        ValsetFileInfo fileInfo = subjectMatchFileInfoGateway.findById(fileId);
        if (fileInfo == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的文件主数据: " + fileId);
        }
        return toView(fileInfo);
    }

    @Override
    public List<ValsetFileInfoViewDTO> searchFileInfos(String sourceChannel,
                                                            String fileStatus,
                                                            String fileFingerprint,
                                                            Integer limit) {
        ValsetFileSourceChannel channel = parseSourceChannel(sourceChannel);
        ValsetFileStatus status = parseFileStatus(fileStatus);
        List<ValsetFileInfo> fileInfos = subjectMatchFileInfoGateway.search(channel, status, fileFingerprint);
        if (fileInfos.isEmpty()) {
            return Collections.emptyList();
        }
        int maxSize = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        return fileInfos.stream()
                .limit(maxSize)
                .map(this::toView)
                .toList();
    }

    @Override
    public List<ValsetFileIngestLogViewDTO> queryIngestLogs(Long fileId) {
        List<ValsetFileIngestLog> ingestLogs = subjectMatchFileIngestLogGateway.findByFileId(fileId);
        if (ingestLogs.isEmpty()) {
            return Collections.emptyList();
        }
        return ingestLogs.stream().map(this::toView).toList();
    }

    @Override
    public List<ValuationSheetStyleViewDTO> querySheetStyles(Long fileId) {
        if (fileId == null) {
            return Collections.emptyList();
        }
        List<ValuationSheetStylePO> stylePOs = valuationSheetStyleMapper.findByFileId(fileId);
        if (stylePOs == null || stylePOs.isEmpty()) {
            return Collections.emptyList();
        }
        return stylePOs.stream()
                .map(this::toView)
                .toList();
    }

    private ValsetFileInfoViewDTO toView(ValsetFileInfo fileInfo) {
        return ValsetFileInfoViewDTO.builder()
                .fileId(fileInfo.getFileId())
                .fileNameOriginal(fileInfo.getFileNameOriginal())
                .fileNameNormalized(fileInfo.getFileNameNormalized())
                .fileExtension(fileInfo.getFileExtension())
                .mimeType(fileInfo.getMimeType())
                .fileSizeBytes(fileInfo.getFileSizeBytes())
                .fileFingerprint(fileInfo.getFileFingerprint())
                .sourceChannel(fileInfo.getSourceChannel() == null ? null : fileInfo.getSourceChannel().name())
                .sourceUri(fileInfo.getSourceUri())
                .storageType(fileInfo.getStorageType() == null ? null : fileInfo.getStorageType().name())
                .storageUri(fileInfo.getStorageUri())
                .fileFormat(fileInfo.getFileFormat())
                .fileStatus(fileInfo.getFileStatus() == null ? null : fileInfo.getFileStatus().name())
                .createdBy(fileInfo.getCreatedBy())
                .receivedAt(fileInfo.getReceivedAt())
                .storedAt(fileInfo.getStoredAt())
                .lastProcessedAt(fileInfo.getLastProcessedAt())
                .lastTaskId(fileInfo.getLastTaskId())
                .errorMessage(fileInfo.getErrorMessage())
                .sourceMetaJson(fileInfo.getSourceMetaJson())
                .storageMetaJson(fileInfo.getStorageMetaJson())
                .remark(fileInfo.getRemark())
                .build();
    }

    private ValsetFileIngestLogViewDTO toView(ValsetFileIngestLog ingestLog) {
        return ValsetFileIngestLogViewDTO.builder()
                .ingestId(ingestLog.getIngestId())
                .fileId(ingestLog.getFileId())
                .sourceChannel(ingestLog.getSourceChannel() == null ? null : ingestLog.getSourceChannel().name())
                .sourceUri(ingestLog.getSourceUri())
                .channelMessageId(ingestLog.getChannelMessageId())
                .ingestStatus(ingestLog.getIngestStatus())
                .ingestTime(ingestLog.getIngestTime())
                .ingestMetaJson(ingestLog.getIngestMetaJson())
                .createdBy(ingestLog.getCreatedBy())
                .errorMessage(ingestLog.getErrorMessage())
                .build();
    }

    private ValuationSheetStyleViewDTO toView(ValuationSheetStylePO stylePO) {
        Map<String, Object> parsed = parseMap(stylePO.getSheetStyleJson());
        List<Map<String, Object>> rows = extractRows(parsed);
        return ValuationSheetStyleViewDTO.builder()
                .id(stylePO.getId())
                .taskId(stylePO.getTaskId())
                .fileId(stylePO.getFileId())
                .sheetName(stylePO.getSheetName())
                .styleScope(stylePO.getStyleScope())
                .sheetStyleJson(stylePO.getSheetStyleJson())
                .titleRows(extractTitleRows(rows))
                .headerRows(extractHeaderRows(rows))
                .mergeAreas(extractMergeAreas(parsed))
                .previewRowCount(stylePO.getPreviewRowCount())
                .createdAt(stylePO.getCreatedAt())
                .build();
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("sheetStyleJson 反序列化失败", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map<String, Object> parsed) {
        Object cellData = parsed.get("cellData");
        if (!(cellData instanceof Map<?, ?> cellDataMap) || cellDataMap.isEmpty()) {
            return List.of();
        }
        return cellDataMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("rowIndex", parseInteger(entry.getKey()));
                    row.put("cells", entry.getValue());
                    row.put("texts", extractRowTexts(entry.getValue()));
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> extractTitleRows(List<Map<String, Object>> rows) {
        int headerRowIndex = findHeaderRowIndex(rows);
        if (headerRowIndex < 0) {
            return rows;
        }
        return rows.stream()
                .filter(row -> {
                    Integer rowIndex = parseInteger(row.get("rowIndex"));
                    return rowIndex != null && rowIndex < headerRowIndex;
                })
                .toList();
    }

    private List<Map<String, Object>> extractHeaderRows(List<Map<String, Object>> rows) {
        int headerRowIndex = findHeaderRowIndex(rows);
        if (headerRowIndex < 0) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> {
                    Integer rowIndex = parseInteger(row.get("rowIndex"));
                    return rowIndex != null && rowIndex >= headerRowIndex;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMergeAreas(Map<String, Object> parsed) {
        Object mergeData = parsed.get("mergeData");
        if (!(mergeData instanceof List<?> mergeList) || mergeList.isEmpty()) {
            return List.of();
        }
        return mergeList.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private int findHeaderRowIndex(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            List<String> texts = (List<String>) row.get("texts");
            if (texts != null && texts.contains("科目代码") && texts.contains("科目名称")) {
                Integer rowIndex = parseInteger(row.get("rowIndex"));
                return rowIndex == null ? -1 : rowIndex;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRowTexts(Object rowCellData) {
        if (!(rowCellData instanceof Map<?, ?> cellMap) || cellMap.isEmpty()) {
            return List.of();
        }
        return cellMap.values().stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .map(value -> {
                    Object text = value.get("v");
                    return text == null ? "" : String.valueOf(text);
                })
                .toList();
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private ValsetFileSourceChannel parseSourceChannel(String sourceChannel) {
        if (sourceChannel == null || sourceChannel.isBlank()) {
            return null;
        }
        try {
            return ValsetFileSourceChannel.valueOf(sourceChannel.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "不支持的 sourceChannel: " + sourceChannel, exception);
        }
    }

    private ValsetFileStatus parseFileStatus(String fileStatus) {
        if (fileStatus == null || fileStatus.isBlank()) {
            return null;
        }
        try {
            return ValsetFileStatus.valueOf(fileStatus.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "不支持的 fileStatus: " + fileStatus, exception);
        }
    }
}
