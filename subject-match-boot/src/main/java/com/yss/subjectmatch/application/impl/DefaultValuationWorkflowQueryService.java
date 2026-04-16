package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.application.dto.DwdExternalValuationViewDTO;
import com.yss.subjectmatch.application.dto.MatchResultViewDTO;
import com.yss.subjectmatch.application.dto.RawValuationDataViewDTO;
import com.yss.subjectmatch.application.dto.RawValuationSheetDTO;
import com.yss.subjectmatch.application.dto.RawValuationRowDTO;
import com.yss.subjectmatch.application.dto.StgExternalValuationViewDTO;
import com.yss.subjectmatch.domain.gateway.DwdExternalValuationGateway;
import com.yss.subjectmatch.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.subjectmatch.application.service.ValuationWorkflowQueryService;
import com.yss.subjectmatch.domain.gateway.MatchResultGateway;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectMatchResult;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.entity.ValuationSheetStylePO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.subjectmatch.extract.repository.mapper.ValuationSheetStyleMapper;
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
    private final SubjectMatchFileInfoGateway subjectMatchFileInfoGateway;
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
    private final StandardizedExternalValuationGateway standardizedExternalValuationGateway;
    private final MatchResultGateway matchResultGateway;
    private final ObjectMapper objectMapper;

    public DefaultValuationWorkflowQueryService(ValuationFileDataMapper valuationFileDataMapper,
                                                ValuationSheetStyleMapper valuationSheetStyleMapper,
                                                SubjectMatchFileInfoGateway subjectMatchFileInfoGateway,
                                                DwdExternalValuationGateway dwdExternalValuationGateway,
                                                StandardizedExternalValuationGateway standardizedExternalValuationGateway,
                                                MatchResultGateway matchResultGateway,
                                                ObjectMapper objectMapper) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.valuationSheetStyleMapper = valuationSheetStyleMapper;
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.dwdExternalValuationGateway = dwdExternalValuationGateway;
        this.standardizedExternalValuationGateway = standardizedExternalValuationGateway;
        this.matchResultGateway = matchResultGateway;
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
                .toList();
        List<RawValuationSheetDTO> sheetViews = buildSheetViews(fileId);
        return RawValuationDataViewDTO.builder()
                .fileId(fileId)
                .totalRows(rows.size())
                .sheets(sheetViews)
                .rows(rowViews)
                .build();
    }

    @Override
    public StgExternalValuationViewDTO queryStgData(Long fileId) {
        ParsedValuationData stgValuationData = dwdExternalValuationGateway.findLatestByFileId(fileId);
        if (stgValuationData == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的 STG 外部估值数据");
        }
        return StgExternalValuationViewDTO.builder()
                .fileId(fileId)
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
                .subjects(stgValuationData.getSubjects())
                .metrics(stgValuationData.getMetrics())
                .build();
    }

    @Override
    public DwdExternalValuationViewDTO queryDwdData(Long fileId) {
        ParsedValuationData stgValuationData = dwdExternalValuationGateway.findLatestByFileId(fileId);
        ParsedValuationData standardizedValuationData = standardizedExternalValuationGateway.findLatestByFileId(fileId);
        if (stgValuationData == null && standardizedValuationData == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的 DWD 外部估值数据");
        }
        ParsedValuationData viewData = standardizedValuationData == null ? stgValuationData : standardizedValuationData;
        return DwdExternalValuationViewDTO.builder()
                .fileId(fileId)
                .workbookPath(stgValuationData == null ? null : stgValuationData.getWorkbookPath())
                .sheetName(stgValuationData == null ? null : stgValuationData.getSheetName())
                .headerRowNumber(stgValuationData == null ? null : stgValuationData.getHeaderRowNumber())
                .dataStartRowNumber(stgValuationData == null ? null : stgValuationData.getDataStartRowNumber())
                .fileNameOriginal(stgValuationData == null ? null : stgValuationData.getFileNameOriginal())
                .title(stgValuationData == null ? null : stgValuationData.getTitle())
                .basicInfo(stgValuationData == null ? null : stgValuationData.getBasicInfo())
                .headers(stgValuationData == null ? null : stgValuationData.getHeaders())
                .headerDetails(stgValuationData == null ? null : stgValuationData.getHeaderDetails())
                .headerColumns(stgValuationData == null ? null : stgValuationData.getHeaderColumns())
                .subjects(viewData.getSubjects())
                .metrics(viewData.getMetrics())
                .build();
    }

    @Override
    public MatchResultViewDTO queryMatchResults(Long fileId) {
        List<SubjectMatchResult> results = matchResultGateway.findByFileId(fileId);
        if (results == null || results.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的匹配结果");
        }
        return MatchResultViewDTO.builder()
                .fileId(fileId)
                .matchedCount(results.size())
                .results(results)
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
                            Collectors.toList()))
                    .entrySet()
                    .stream()
                    .map(entry -> RawValuationSheetDTO.builder()
                            .sheetName(entry.getKey())
                            .headerMeta(entry.getValue().stream()
                                    .map(ValuationSheetStylePO::getSheetStyleJson)
                                    .filter(value -> value != null && !value.isBlank())
                                    .findFirst()
                                    .map(this::parseMap)
                                    .orElse(null))
                            .build())
                    .toList();
        }

        String fallbackSheetName = "Sheet1";
        try {
            var fileInfo = subjectMatchFileInfoGateway.findById(fileId);
            if (fileInfo != null && fileInfo.getFileFormat() != null && !fileInfo.getFileFormat().isBlank()) {
                fallbackSheetName = fileInfo.getFileFormat();
            }
        } catch (Exception ignored) {
            // fallback
        }
        return List.of(RawValuationSheetDTO.builder()
                .sheetName(fallbackSheetName)
                .headerMeta(null)
                .build());
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
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
