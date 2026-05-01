package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.gateway.DwdExternalValuationGateway;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationBasicInfoRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationHeaderRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationMetricRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationSubjectRepository;
import com.yss.valset.extract.repository.entity.DwdExternalValuationBasicInfoPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationHeaderPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationMetricPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationSubjectPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DWD 外部估值标准数据持久化网关实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DwdExternalValuationGatewayImpl implements DwdExternalValuationGateway {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DwdExternalValuationRepository valuationRepository;
    private final DwdExternalValuationBasicInfoRepository basicInfoRepository;
    private final DwdExternalValuationHeaderRepository headerRepository;
    private final DwdExternalValuationSubjectRepository subjectRepository;
    private final DwdExternalValuationMetricRepository metricRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveDwdExternalValuation(Long taskId, Long fileId, ParsedValuationData parsedValuationData) {
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
        DwdExternalValuationPO valuationPO = new DwdExternalValuationPO();
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
        log.info("DWD 外部估值标准数据落地完成，taskId={}, fileId={}, valuationId={}", taskId, fileId, valuationId);
    }

    @Override
    public ParsedValuationData findLatestByFileId(Long fileId) {
        DwdExternalValuationPO valuationPO = valuationRepository.selectOne(
                Wrappers.lambdaQuery(DwdExternalValuationPO.class)
                        .eq(DwdExternalValuationPO::getFileId, fileId)
                        .orderByDesc(DwdExternalValuationPO::getId)
                        .last("limit 1")
        );
        if (valuationPO == null) {
            return null;
        }
        if (valuationPO.getHeaderRowNumber() == null || valuationPO.getDataStartRowNumber() == null) {
            log.warn("DWD 外部估值记录缺少表头行号或数据起始行号，视为无效记录，fileId={}, valuationId={}, headerRowNumber={}, dataStartRowNumber={}",
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

    private void saveBasicInfos(Long valuationId, Map<String, String> basicInfo) {
        if (basicInfo == null || basicInfo.isEmpty()) {
            return;
        }
        List<DwdExternalValuationBasicInfoPO> poList = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, String> entry : basicInfo.entrySet()) {
            DwdExternalValuationBasicInfoPO po = new DwdExternalValuationBasicInfoPO();
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
        List<DwdExternalValuationHeaderPO> poList = new ArrayList<>(headers.size());
        for (int index = 0; index < headers.size(); index++) {
            DwdExternalValuationHeaderPO po = new DwdExternalValuationHeaderPO();
            po.setValuationId(valuationId);
            po.setColumnIndex(index);
            po.setHeaderName(headers.get(index));
            List<String> detail = headerDetails != null && index < headerDetails.size()
                    ? headerDetails.get(index)
                    : List.of(headers.get(index));
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
        List<DwdExternalValuationSubjectPO> poList = subjects.stream()
                .map(subject -> {
                    DwdExternalValuationSubjectPO po = new DwdExternalValuationSubjectPO();
                    po.setValuationId(valuationId);
                    po.setSheetName(subject.getSheetName());
                    po.setRowDataNumber(subject.getRowDataNumber());
                    po.setSubjectCode(subject.getSubjectCode());
                    po.setSubjectName(subject.getSubjectName());
                    po.setLevel(subject.getLevel());
                    po.setParentCode(subject.getParentCode());
                    po.setRootCode(subject.getRootCode());
                    po.setSegmentCount(subject.getSegmentCount());
                    po.setPathCodesJson(writeJson(subject.getPathCodes()));
                    po.setLeaf(subject.getLeaf());
                    po.setRawValuesJson(writeJson(subject.getRawValues()));
                    return po;
                })
                .toList();
        subjectRepository.insertBatchSomeColumn(poList);
    }

    private void saveMetrics(Long valuationId, List<MetricRecord> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        List<DwdExternalValuationMetricPO> poList = metrics.stream()
                .map(metric -> {
                    DwdExternalValuationMetricPO po = new DwdExternalValuationMetricPO();
                    po.setValuationId(valuationId);
                    po.setSheetName(metric.getSheetName());
                    po.setRowDataNumber(metric.getRowDataNumber());
                    po.setMetricName(metric.getMetricName());
                    po.setMetricType(metric.getMetricType());
                    po.setMetricValue(metric.getValue());
                    po.setRawValuesJson(writeJson(metric.getRawValues()));
                    return po;
                })
                .toList();
        metricRepository.insertBatchSomeColumn(poList);
    }

    private Map<String, String> loadBasicInfo(Long valuationId) {
        List<DwdExternalValuationBasicInfoPO> poList = basicInfoRepository.selectList(
                Wrappers.lambdaQuery(DwdExternalValuationBasicInfoPO.class)
                        .eq(DwdExternalValuationBasicInfoPO::getValuationId, valuationId)
                        .orderByAsc(DwdExternalValuationBasicInfoPO::getSortOrder)
        );
        Map<String, String> result = new LinkedHashMap<>();
        for (DwdExternalValuationBasicInfoPO po : poList) {
            result.put(po.getInfoKey(), po.getInfoValue());
        }
        return result;
    }

    private List<String> loadHeaders(Long valuationId) {
        return headerRepository.selectList(
                        Wrappers.lambdaQuery(DwdExternalValuationHeaderPO.class)
                                .eq(DwdExternalValuationHeaderPO::getValuationId, valuationId)
                                .orderByAsc(DwdExternalValuationHeaderPO::getColumnIndex)
                ).stream()
                .map(DwdExternalValuationHeaderPO::getHeaderName)
                .toList();
    }

    private List<List<String>> loadHeaderDetails(Long valuationId) {
        return headerRepository.selectList(
                        Wrappers.lambdaQuery(DwdExternalValuationHeaderPO.class)
                                .eq(DwdExternalValuationHeaderPO::getValuationId, valuationId)
                                .orderByAsc(DwdExternalValuationHeaderPO::getColumnIndex)
                ).stream()
                .map(po -> readJson(po.getHeaderDetailJson(), STRING_LIST_TYPE))
                .toList();
    }

    private List<HeaderColumnMeta> loadHeaderColumns(Long valuationId) {
        List<DwdExternalValuationHeaderPO> headerPOList = headerRepository.selectList(
                Wrappers.lambdaQuery(DwdExternalValuationHeaderPO.class)
                        .eq(DwdExternalValuationHeaderPO::getValuationId, valuationId)
                        .orderByAsc(DwdExternalValuationHeaderPO::getColumnIndex)
        );
        if (headerPOList == null || headerPOList.isEmpty()) {
            return List.of();
        }
        List<HeaderColumnMeta> result = new ArrayList<>(headerPOList.size());
        for (DwdExternalValuationHeaderPO po : headerPOList) {
            result.add(readHeaderColumnMeta(po));
        }
        return result;
    }

    private List<SubjectRecord> loadSubjects(Long valuationId) {
        return subjectRepository.selectList(
                        Wrappers.lambdaQuery(DwdExternalValuationSubjectPO.class)
                                .eq(DwdExternalValuationSubjectPO::getValuationId, valuationId)
                                .orderByAsc(DwdExternalValuationSubjectPO::getRowDataNumber)
                ).stream()
                .map(po -> SubjectRecord.builder()
                        .sheetName(po.getSheetName())
                        .rowDataNumber(po.getRowDataNumber())
                        .subjectCode(po.getSubjectCode())
                        .subjectName(po.getSubjectName())
                        .level(po.getLevel())
                        .parentCode(po.getParentCode())
                        .rootCode(po.getRootCode())
                        .segmentCount(po.getSegmentCount())
                        .pathCodes(readJson(po.getPathCodesJson(), STRING_LIST_TYPE))
                        .rawValues(readJson(po.getRawValuesJson(), new TypeReference<List<Object>>() {
                        }))
                        .leaf(po.getLeaf())
                        .build())
                .toList();
    }

    private List<MetricRecord> loadMetrics(Long valuationId) {
        return metricRepository.selectList(
                        Wrappers.lambdaQuery(DwdExternalValuationMetricPO.class)
                                .eq(DwdExternalValuationMetricPO::getValuationId, valuationId)
                                .orderByAsc(DwdExternalValuationMetricPO::getRowDataNumber)
                ).stream()
                .map(po -> MetricRecord.builder()
                        .sheetName(po.getSheetName())
                        .rowDataNumber(po.getRowDataNumber())
                        .metricName(po.getMetricName())
                        .metricType(po.getMetricType())
                        .value(po.getMetricValue())
                        .rawValues(readJson(po.getRawValuesJson(), MAP_TYPE))
                        .build())
                .toList();
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("DWD 外部估值标准数据序列化失败", exception);
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
                                : headerName == null || headerName.isBlank())
                        .build();
            }
        }
        return HeaderColumnMeta.builder()
                .columnIndex(columnIndex)
                .headerName(headerName)
                .headerPath(headerName)
                .pathSegments(headerDetail)
                .blankColumn(headerName == null || headerName.isBlank())
                .build();
    }

    private HeaderColumnMeta readHeaderColumnMeta(DwdExternalValuationHeaderPO po) {
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
                .pathSegments(detail == null ? List.of() : detail)
                .blankColumn(po.getHeaderName() == null || po.getHeaderName().isBlank())
                .build();
    }

    private String defaultString(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("DWD 外部估值标准数据反序列化失败", exception);
        }
    }
}
