package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.gateway.StgExternalValuationGateway;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.mapper.StgExternalValuationBasicInfoRepository;
import com.yss.valset.extract.repository.mapper.StgExternalValuationHeaderRepository;
import com.yss.valset.extract.repository.mapper.StgExternalValuationMetricRepository;
import com.yss.valset.extract.repository.mapper.StgExternalValuationRepository;
import com.yss.valset.extract.repository.mapper.StgExternalValuationSubjectRepository;
import com.yss.valset.extract.repository.entity.StgExternalValuationBasicInfoPO;
import com.yss.valset.extract.repository.entity.StgExternalValuationHeaderPO;
import com.yss.valset.extract.repository.entity.StgExternalValuationMetricPO;
import com.yss.valset.extract.repository.entity.StgExternalValuationPO;
import com.yss.valset.extract.repository.entity.StgExternalValuationSubjectPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * STG 外部估值解析快照持久化网关实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StgExternalValuationGatewayImpl implements StgExternalValuationGateway {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final StgExternalValuationRepository valuationRepository;
    private final StgExternalValuationBasicInfoRepository basicInfoRepository;
    private final StgExternalValuationHeaderRepository headerRepository;
    private final StgExternalValuationSubjectRepository subjectRepository;
    private final StgExternalValuationMetricRepository metricRepository;
    private final ObjectMapper objectMapper;
    private final DatabaseDialectSupport databaseDialectSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveStgExternalValuation(Long taskId, Long fileId, ParsedValuationData parsedValuationData) {
        if (fileId == null) {
            throw new IllegalStateException("解析文件ID为空，无法覆盖贴源快照，taskId=" + taskId);
        }
        if (parsedValuationData == null) {
            throw new IllegalStateException("解析结果为空，无法落库，taskId=" + taskId + ", fileId=" + fileId);
        }
        if (parsedValuationData.getHeaderRowNumber() == null || parsedValuationData.getDataStartRowNumber() == null) {
            throw new IllegalStateException("解析结果缺少表头行号或数据起始行号，无法落库，taskId="
                    + taskId
                    + ", fileId=" + fileId
                    + ", headerRowNumber=" + parsedValuationData.getHeaderRowNumber()
                    + ", dataStartRowNumber=" + parsedValuationData.getDataStartRowNumber());
        }
        deleteOldSnapshot(fileId);

        StgExternalValuationPO valuationPO = new StgExternalValuationPO();
        valuationPO.setTaskId(taskId);
        valuationPO.setFileId(fileId);
        valuationPO.setWorkbookPath(parsedValuationData.getWorkbookPath());
        valuationPO.setSheetName(parsedValuationData.getSheetName());
        valuationPO.setHeaderRowNumber(parsedValuationData.getHeaderRowNumber());
        valuationPO.setDataStartRowNumber(parsedValuationData.getDataStartRowNumber());
        valuationPO.setTitle(parsedValuationData.getTitle());
        valuationRepository.insert(valuationPO);

        Long valuationId = valuationPO.getId();
        saveBasicInfos(valuationId, parsedValuationData.getBasicInfo());
        saveHeaders(valuationId,
                parsedValuationData.getHeaders(),
                parsedValuationData.getHeaderDetails(),
                parsedValuationData.getHeaderColumns());
        saveSubjects(valuationId, parsedValuationData.getSubjects());
        saveMetrics(valuationId, parsedValuationData.getMetrics());
        log.info("STG 外部估值贴源数据落地完成，taskId={}, fileId={}, valuationId={}", taskId, fileId, valuationId);
    }

    @Override
    public ParsedValuationData findLatestByFileId(Long fileId) {
        StgExternalValuationPO valuationPO = valuationRepository.selectOne(
                Wrappers.lambdaQuery(StgExternalValuationPO.class)
                        .eq(StgExternalValuationPO::getFileId, fileId)
                        .orderByDesc(StgExternalValuationPO::getId)
                        .last(databaseDialectSupport.limitClause(1))
        );
        if (valuationPO == null) {
            return null;
        }
        if (valuationPO.getHeaderRowNumber() == null || valuationPO.getDataStartRowNumber() == null) {
            log.warn("STG 外部估值记录缺少表头行号或数据起始行号，视为无效记录，fileId={}, valuationId={}, headerRowNumber={}, dataStartRowNumber={}",
                    fileId,
                    valuationPO.getId(),
                    valuationPO.getHeaderRowNumber(),
                    valuationPO.getDataStartRowNumber());
            return null;
        }
        Long valuationId = valuationPO.getId();
        return ParsedValuationData.builder()
                .workbookPath(valuationPO.getWorkbookPath())
                .sheetName(valuationPO.getSheetName())
                .headerRowNumber(valuationPO.getHeaderRowNumber())
                .dataStartRowNumber(valuationPO.getDataStartRowNumber())
                .fileNameOriginal(valuationPO.getTitle())
                .title(valuationPO.getTitle())
                .basicInfo(loadBasicInfo(valuationId))
                .headers(loadHeaders(valuationId))
                .headerDetails(loadHeaderDetails(valuationId))
                .headerColumns(loadHeaderColumns(valuationId))
                .subjects(loadSubjects(valuationId))
                .metrics(loadMetrics(valuationId))
                .build();
    }

    private void deleteOldSnapshot(Long fileId) {
        List<Long> oldValuationIds = valuationRepository.selectList(
                        Wrappers.lambdaQuery(StgExternalValuationPO.class)
                                .eq(StgExternalValuationPO::getFileId, fileId)
                ).stream()
                .map(StgExternalValuationPO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
        if (!oldValuationIds.isEmpty()) {
            basicInfoRepository.delete(Wrappers.lambdaQuery(StgExternalValuationBasicInfoPO.class)
                    .in(StgExternalValuationBasicInfoPO::getValuationId, oldValuationIds));
            headerRepository.delete(Wrappers.lambdaQuery(StgExternalValuationHeaderPO.class)
                    .in(StgExternalValuationHeaderPO::getValuationId, oldValuationIds));
            metricRepository.delete(Wrappers.lambdaQuery(StgExternalValuationMetricPO.class)
                    .in(StgExternalValuationMetricPO::getValuationId, oldValuationIds));
            subjectRepository.delete(Wrappers.lambdaQuery(StgExternalValuationSubjectPO.class)
                    .in(StgExternalValuationSubjectPO::getValuationId, oldValuationIds));
        }
        valuationRepository.delete(Wrappers.lambdaQuery(StgExternalValuationPO.class)
                .eq(StgExternalValuationPO::getFileId, fileId));
    }

    private void saveBasicInfos(Long valuationId, Map<String, String> basicInfo) {
        if (basicInfo == null || basicInfo.isEmpty()) {
            return;
        }
        List<StgExternalValuationBasicInfoPO> poList = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, String> entry : basicInfo.entrySet()) {
            StgExternalValuationBasicInfoPO po = new StgExternalValuationBasicInfoPO();
            po.setValuationId(valuationId);
            po.setSortOrder(order++);
            po.setInfoKey(entry.getKey());
            po.setInfoValue(entry.getValue());
            poList.add(po);
        }
        basicInfoRepository.insertBatchSomeColumn(poList);
    }

    private void saveHeaders(Long valuationId, List<String> headers, List<List<String>> headerDetails, List<HeaderColumnMeta> headerColumns) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        List<StgExternalValuationHeaderPO> poList = new ArrayList<>(headers.size());
        for (int index = 0; index < headers.size(); index++) {
            StgExternalValuationHeaderPO po = new StgExternalValuationHeaderPO();
            po.setValuationId(valuationId);
            po.setColumnIndex(index);
            po.setHeaderName(headers.get(index));
            List<String> detail = headerDetails != null && index < headerDetails.size()
                    ? headerDetails.get(index)
                    : java.util.Arrays.asList(headers.get(index));
            po.setHeaderDetailJson(writeJson(detail));
            po.setHeaderColumnMetaJson(writeJson(resolveHeaderColumnMeta(index, headers.get(index), detail, headerColumns)));
            poList.add(po);
        }
        headerRepository.insertBatchSomeColumn(poList);
    }

    private void saveSubjects(Long valuationId, List<SubjectRecord> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        List<StgExternalValuationSubjectPO> poList = subjects.stream()
                .map(subject -> {
                    StgExternalValuationSubjectPO po = new StgExternalValuationSubjectPO();
                    po.setValuationId(valuationId);
                    po.setSheetName(subject.getSheetName());
                    po.setRowDataNumber(subject.getRowDataNumber());
                    po.setSubjectCode(subject.getSubjectCode());
                    po.setSubjectName(subject.getSubjectName());
                    po.setLevelNo(subject.getLevel());
                    po.setParentCode(subject.getParentCode());
                    po.setRootCode(subject.getRootCode());
                    po.setSegmentCount(subject.getSegmentCount());
                    po.setPathCodesJson(writeJson(subject.getPathCodes()));
                    po.setLeaf(subject.getLeaf());
                    po.setRawValuesJson(writeJson(subject.getRawValues()));
                    return po;
                })
                .collect(java.util.stream.Collectors.toList());
        subjectRepository.insertBatchSomeColumn(poList);
    }

    private void saveMetrics(Long valuationId, List<MetricRecord> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        List<StgExternalValuationMetricPO> poList = metrics.stream()
                .map(metric -> {
                    StgExternalValuationMetricPO po = new StgExternalValuationMetricPO();
                    po.setValuationId(valuationId);
                    po.setSheetName(metric.getSheetName());
                    po.setRowDataNumber(metric.getRowDataNumber());
                    po.setMetricName(metric.getMetricName());
                    po.setMetricType(metric.getMetricType());
                    po.setMetricValue(metric.getValue());
                    po.setRawValuesJson(writeJson(metric.getRawValues()));
                    return po;
                })
                .collect(java.util.stream.Collectors.toList());
        metricRepository.insertBatchSomeColumn(poList);
    }

    private Map<String, String> loadBasicInfo(Long valuationId) {
        List<StgExternalValuationBasicInfoPO> poList = basicInfoRepository.selectList(
                Wrappers.lambdaQuery(StgExternalValuationBasicInfoPO.class)
                        .eq(StgExternalValuationBasicInfoPO::getValuationId, valuationId)
                        .orderByAsc(StgExternalValuationBasicInfoPO::getSortOrder)
        );
        Map<String, String> result = new LinkedHashMap<>();
        for (StgExternalValuationBasicInfoPO po : poList) {
            result.put(po.getInfoKey(), po.getInfoValue());
        }
        return result;
    }

    private List<String> loadHeaders(Long valuationId) {
        return headerRepository.selectList(
                        Wrappers.lambdaQuery(StgExternalValuationHeaderPO.class)
                                .eq(StgExternalValuationHeaderPO::getValuationId, valuationId)
                                .orderByAsc(StgExternalValuationHeaderPO::getColumnIndex)
                ).stream()
                .map(StgExternalValuationHeaderPO::getHeaderName)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<List<String>> loadHeaderDetails(Long valuationId) {
        return headerRepository.selectList(
                        Wrappers.lambdaQuery(StgExternalValuationHeaderPO.class)
                                .eq(StgExternalValuationHeaderPO::getValuationId, valuationId)
                                .orderByAsc(StgExternalValuationHeaderPO::getColumnIndex)
                ).stream()
                .map(po -> readJson(po.getHeaderDetailJson(), STRING_LIST_TYPE))
                .collect(java.util.stream.Collectors.toList());
    }

    private List<HeaderColumnMeta> loadHeaderColumns(Long valuationId) {
        List<StgExternalValuationHeaderPO> headerPOList = headerRepository.selectList(
                Wrappers.lambdaQuery(StgExternalValuationHeaderPO.class)
                        .eq(StgExternalValuationHeaderPO::getValuationId, valuationId)
                        .orderByAsc(StgExternalValuationHeaderPO::getColumnIndex)
        );
        if (headerPOList == null || headerPOList.isEmpty()) {
            return java.util.Arrays.asList();
        }
        List<HeaderColumnMeta> result = new ArrayList<>(headerPOList.size());
        for (StgExternalValuationHeaderPO po : headerPOList) {
            result.add(readHeaderColumnMeta(po));
        }
        return result;
    }

    private List<SubjectRecord> loadSubjects(Long valuationId) {
        return subjectRepository.selectList(
                        Wrappers.lambdaQuery(StgExternalValuationSubjectPO.class)
                                .eq(StgExternalValuationSubjectPO::getValuationId, valuationId)
                                .orderByAsc(StgExternalValuationSubjectPO::getRowDataNumber)
                ).stream()
                .map(po -> SubjectRecord.builder()
                        .sheetName(po.getSheetName())
                        .rowDataNumber(po.getRowDataNumber())
                        .subjectCode(po.getSubjectCode())
                        .subjectName(po.getSubjectName())
                        .level(po.getLevelNo())
                        .parentCode(po.getParentCode())
                        .rootCode(po.getRootCode())
                        .segmentCount(po.getSegmentCount())
                        .pathCodes(readJson(po.getPathCodesJson(), STRING_LIST_TYPE))
                        .rawValues(readJson(po.getRawValuesJson(), new TypeReference<List<Object>>() {
                        }))
                        .leaf(po.getLeaf())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private List<MetricRecord> loadMetrics(Long valuationId) {
        return metricRepository.selectList(
                        Wrappers.lambdaQuery(StgExternalValuationMetricPO.class)
                                .eq(StgExternalValuationMetricPO::getValuationId, valuationId)
                                .orderByAsc(StgExternalValuationMetricPO::getRowDataNumber)
                ).stream()
                .map(po -> MetricRecord.builder()
                        .sheetName(po.getSheetName())
                        .rowDataNumber(po.getRowDataNumber())
                        .metricName(po.getMetricName())
                        .metricType(po.getMetricType())
                        .value(po.getMetricValue())
                        .rawValues(readJson(po.getRawValuesJson(), MAP_TYPE))
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("STG 外部估值解析快照序列化失败", exception);
        }
    }

    private HeaderColumnMeta resolveHeaderColumnMeta(
            int columnIndex,
            String headerName,
            List<String> headerDetail,
            List<HeaderColumnMeta> headerColumns
    ) {
        if (headerColumns != null && columnIndex < headerColumns.size()) {
            HeaderColumnMeta headerColumnMeta = headerColumns.get(columnIndex);
            if (headerColumnMeta != null) {
                return HeaderColumnMeta.builder()
                        .columnIndex(columnIndex)
                        .headerName(defaultString(headerColumnMeta.getHeaderName(), headerName))
                        .headerPath(defaultString(headerColumnMeta.getHeaderPath(), headerName))
                        .pathSegments(headerColumnMeta.getPathSegments() == null ? headerDetail : headerColumnMeta.getPathSegments())
                        .blankColumn(headerColumnMeta.getBlankColumn() != null
                                ? headerColumnMeta.getBlankColumn()
                                : headerName == null || headerName.trim().isEmpty())
                        .build();
            }
        }
        return HeaderColumnMeta.builder()
                .columnIndex(columnIndex)
                .headerName(headerName)
                .headerPath(headerName)
                .pathSegments(headerDetail)
                .blankColumn(headerName == null || headerName.trim().isEmpty())
                .build();
    }

    private HeaderColumnMeta readHeaderColumnMeta(StgExternalValuationHeaderPO po) {
        HeaderColumnMeta meta = readJson(po.getHeaderColumnMetaJson(), new TypeReference<HeaderColumnMeta>() {
        });
        if (meta != null) {
            return meta;
        }
        List<String> detail = readJson(po.getHeaderDetailJson(), STRING_LIST_TYPE);
        return HeaderColumnMeta.builder()
                .columnIndex(po.getColumnIndex())
                .headerName(po.getHeaderName())
                .headerPath(po.getHeaderName())
                .pathSegments(detail == null ? java.util.Arrays.asList() : detail)
                .blankColumn(po.getHeaderName() == null || po.getHeaderName().trim().isEmpty())
                .build();
    }

    private String defaultString(String candidate, String fallback) {
        return candidate == null || candidate.trim().isEmpty() ? fallback : candidate;
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return objectMapper.readValue(value, typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("STG 外部估值解析快照反序列化失败", exception);
        }
    }
}
