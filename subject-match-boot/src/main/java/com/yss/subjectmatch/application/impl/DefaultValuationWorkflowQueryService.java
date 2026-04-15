package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.application.dto.DwdExternalValuationViewDTO;
import com.yss.subjectmatch.application.dto.MatchResultViewDTO;
import com.yss.subjectmatch.application.dto.RawValuationDataViewDTO;
import com.yss.subjectmatch.application.dto.RawValuationSheetDTO;
import com.yss.subjectmatch.application.dto.RawValuationRowDTO;
import com.yss.subjectmatch.domain.gateway.DwdExternalValuationGateway;
import com.yss.subjectmatch.application.service.ValuationWorkflowQueryService;
import com.yss.subjectmatch.domain.gateway.MatchResultGateway;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectMatchResult;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
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
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
    private final MatchResultGateway matchResultGateway;
    private final ObjectMapper objectMapper;

    public DefaultValuationWorkflowQueryService(ValuationFileDataMapper valuationFileDataMapper,
                                                DwdExternalValuationGateway dwdExternalValuationGateway,
                                                MatchResultGateway matchResultGateway,
                                                ObjectMapper objectMapper) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.dwdExternalValuationGateway = dwdExternalValuationGateway;
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
                        .sheetName(row.getSheetName())
                        .rowDataNumber(row.getRowDataNumber())
                        .rowData(parseRowData(row.getRowDataJson()))
                        .rowUniverData(parseMap(row.getRowUniverJson()))
                        .build())
                .toList();
        Map<String, List<ValuationFileDataPO>> rowsBySheet = rows.stream()
                .collect(Collectors.groupingBy(row -> row.getSheetName() == null ? "Sheet1" : row.getSheetName(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<RawValuationSheetDTO> sheetViews = rowsBySheet.entrySet()
                .stream()
                .map(entry -> RawValuationSheetDTO.builder()
                        .sheetName(entry.getKey())
                        .headerMeta(entry.getValue().stream()
                                .map(ValuationFileDataPO::getHeaderMetaJson)
                                .filter(value -> value != null && !value.isBlank())
                                .findFirst()
                                .map(this::parseMap)
                                .orElse(null))
                        .build())
                .toList();
        return RawValuationDataViewDTO.builder()
                .fileId(fileId)
                .totalRows(rows.size())
                .sheets(sheetViews)
                .rows(rowViews)
                .build();
    }

    @Override
    public DwdExternalValuationViewDTO queryDwdData(Long fileId) {
        ParsedValuationData parsedValuationData = dwdExternalValuationGateway.findLatestByFileId(fileId);
        if (parsedValuationData == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的 DWD 外部估值数据");
        }
        return DwdExternalValuationViewDTO.builder()
                .fileId(fileId)
                .workbookPath(parsedValuationData.getWorkbookPath())
                .sheetName(parsedValuationData.getSheetName())
                .headerRowNumber(parsedValuationData.getHeaderRowNumber())
                .dataStartRowNumber(parsedValuationData.getDataStartRowNumber())
                .title(parsedValuationData.getTitle())
                .basicInfo(parsedValuationData.getBasicInfo())
                .headers(parsedValuationData.getHeaders())
                .headerDetails(parsedValuationData.getHeaderDetails())
                .headerColumns(parsedValuationData.getHeaderColumns())
                .subjects(parsedValuationData.getSubjects())
                .metrics(parsedValuationData.getMetrics())
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
