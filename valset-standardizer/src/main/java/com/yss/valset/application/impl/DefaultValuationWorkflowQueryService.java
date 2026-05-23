package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.dto.RawValuationDataViewDTO;
import com.yss.valset.application.dto.RawValuationSheetDTO;
import com.yss.valset.application.dto.RawValuationRowDTO;
import com.yss.valset.application.dto.StgExternalValuationViewDTO;
import com.yss.valset.domain.gateway.StgExternalValuationGateway;
import com.yss.valset.application.service.ValuationWorkflowQueryService;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.extract.repository.entity.ValuationFileDataPO;
import com.yss.valset.extract.repository.entity.ValuationSheetStylePO;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import com.yss.valset.extract.standardization.ExternalValuationStandardizationService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 外部估值全流程查询服务默认实现。
 */
@Service
public class DefaultValuationWorkflowQueryService implements ValuationWorkflowQueryService {

    private final ValuationFileDataMapper valuationFileDataMapper;
    private final ValuationSheetStyleMapper valuationSheetStyleMapper;
    private final ValsetFileInfoGateway subjectMatchFileInfoGateway;
    private final StgExternalValuationGateway stgExternalValuationGateway;
    private final ExternalValuationStandardizationService standardizationService;
    private final ObjectMapper objectMapper;

    public DefaultValuationWorkflowQueryService(ValuationFileDataMapper valuationFileDataMapper,
                                                ValuationSheetStyleMapper valuationSheetStyleMapper,
                                                ValsetFileInfoGateway subjectMatchFileInfoGateway,
                                                StgExternalValuationGateway stgExternalValuationGateway,
                                                ExternalValuationStandardizationService standardizationService,
                                                ObjectMapper objectMapper) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.valuationSheetStyleMapper = valuationSheetStyleMapper;
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.stgExternalValuationGateway = stgExternalValuationGateway;
        this.standardizationService = standardizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RawValuationDataViewDTO queryRawData(Long fileId, Integer limit) {
        List<ValuationFileDataPO> rows = valuationFileDataMapper.findByFileId(fileId);
        if (rows == null || rows.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的 ODS 原始数据");
        }
        int safeLimit = limit == null || limit <= 0 ? 200 : Math.min(limit, 1000);
        List<RawValuationRowDTO> rowViews = rows.stream()
                .limit(safeLimit)
                .map(row -> RawValuationRowDTO.builder()
                        .rowDataNumber(row.getRowDataNumber())
                        .rowData(parseRowData(row.getRowDataJson()))
                        .build())
                .collect(java.util.stream.Collectors.toList());
        List<RawValuationSheetDTO> sheetViews = buildSheetViews(fileId);
        return RawValuationDataViewDTO.builder()
                .fileId(fileId == null ? null : String.valueOf(fileId))
                .totalRows(rows.size())
                .sheets(sheetViews)
                .rows(rowViews)
                .build();
    }

    @Override
    public StgExternalValuationViewDTO queryStgData(Long fileId) {
        ParsedValuationData stgValuationData = stgExternalValuationGateway.findLatestByFileId(fileId);
        if (stgValuationData == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的 STG 外部估值数据");
        }
        ParsedValuationData viewData = standardizationService.standardize(stgValuationData);
        return StgExternalValuationViewDTO.builder()
                .fileId(fileId == null ? null : String.valueOf(fileId))
                .workbookPath(stgValuationData.getWorkbookPath())
                .sheetName(stgValuationData.getSheetName())
                .headerRowNumber(stgValuationData.getHeaderRowNumber())
                .dataStartRowNumber(stgValuationData.getDataStartRowNumber())
                .fileNameOriginal(stgValuationData.getFileNameOriginal())
                .title(stgValuationData.getTitle())
                .basicInfo(stgValuationData.getBasicInfo())
                .headers(stgValuationData.getHeaders())
                .headerDetails(stgValuationData.getHeaderDetails())
                .headerColumns(stgValuationData.getHeaderColumns())
                .subjects(viewData.getSubjects())
                .metrics(viewData.getMetrics())
                .build();
    }

    private List<Object> parseRowData(String rowDataJson) {
        try {
            return objectMapper.readValue(rowDataJson, new TypeReference<List<Object>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("ODS 原始行数据反序列化失败", exception);
        }
    }

    private List<RawValuationSheetDTO> buildSheetViews(Long fileId) {
        List<ValuationSheetStylePO> sheetStyles = valuationSheetStyleMapper.findByFileId(fileId);
        if (sheetStyles != null && !sheetStyles.isEmpty()) {
            return sheetStyles.stream()
                    .collect(Collectors.groupingBy(ValuationSheetStylePO::getSheetName,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()))
                    .entrySet()
                    .stream()
                    .map(entry -> RawValuationSheetDTO.builder()
                            .sheetName(entry.getKey())
                            .headerMeta(entry.getValue().stream()
                                    .map(ValuationSheetStylePO::getSheetStyleJson)
                                    .filter(value -> value != null && !value.trim().isEmpty())
                                    .findFirst()
                                    .map(this::parseMap)
                                    .orElse(null))
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        }

        String fallbackSheetName = "Sheet1";
        try {
            ValsetFileInfo fileInfo = subjectMatchFileInfoGateway.findById(fileId);
            if (fileInfo != null && fileInfo.getFileFormat() != null && !fileInfo.getFileFormat().trim().isEmpty()) {
                fallbackSheetName = fileInfo.getFileFormat();
            }
        } catch (Exception ignored) {
            // fallback
        }
        return java.util.Arrays.asList(RawValuationSheetDTO.builder()
                .sheetName(fallbackSheetName)
                .headerMeta(null)
                .build());
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("JSON 反序列化失败", exception);
        }
    }
}
