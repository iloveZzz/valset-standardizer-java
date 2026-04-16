package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.extract.repository.entity.DwdExternalValuationStandardMetricPO;
import com.yss.subjectmatch.extract.repository.entity.DwdExternalValuationStandardSubjectPO;
import com.yss.subjectmatch.extract.repository.mapper.DwdExternalValuationStandardMetricRepository;
import com.yss.subjectmatch.extract.repository.mapper.DwdExternalValuationStandardSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部估值标准化结果持久化网关实现。
 */
@Repository
@RequiredArgsConstructor
public class StandardizedExternalValuationGatewayImpl implements StandardizedExternalValuationGateway {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DwdExternalValuationStandardSubjectRepository subjectRepository;
    private final DwdExternalValuationStandardMetricRepository metricRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveStandardizedExternalValuation(Long valuationId, Long fileId, ParsedValuationData standardizedValuationData) {
        if (standardizedValuationData == null) {
            return;
        }
        saveSubjects(valuationId, fileId, standardizedValuationData.getSubjects());
        saveMetrics(valuationId, fileId, standardizedValuationData.getMetrics());
    }

    @Override
    public ParsedValuationData findLatestByFileId(Long fileId) {
        List<DwdExternalValuationStandardSubjectPO> subjectPOList = subjectRepository.selectList(
                Wrappers.lambdaQuery(DwdExternalValuationStandardSubjectPO.class)
                        .eq(DwdExternalValuationStandardSubjectPO::getFileId, fileId)
                        .orderByAsc(DwdExternalValuationStandardSubjectPO::getRowDataNumber)
        );
        List<DwdExternalValuationStandardMetricPO> metricPOList = metricRepository.selectList(
                Wrappers.lambdaQuery(DwdExternalValuationStandardMetricPO.class)
                        .eq(DwdExternalValuationStandardMetricPO::getFileId, fileId)
                        .orderByAsc(DwdExternalValuationStandardMetricPO::getRowDataNumber)
        );
        if ((subjectPOList == null || subjectPOList.isEmpty()) && (metricPOList == null || metricPOList.isEmpty())) {
            return null;
        }
        return ParsedValuationData.builder()
                .subjects(loadSubjects(subjectPOList))
                .metrics(loadMetrics(metricPOList))
                .build();
    }

    private void saveSubjects(Long valuationId, Long fileId, List<SubjectRecord> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        List<DwdExternalValuationStandardSubjectPO> poList = new ArrayList<>(subjects.size());
        for (SubjectRecord subject : subjects) {
            DwdExternalValuationStandardSubjectPO po = new DwdExternalValuationStandardSubjectPO();
            po.setValuationId(valuationId);
            po.setFileId(fileId);
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
            po.setStandardCode(subject.getStandardCode());
            po.setStandardName(subject.getStandardName());
            po.setStandardValuesJson(writeJson(subject.getStandardValues()));
            po.setMappingRuleId(subject.getMappingRuleId());
            po.setMappingSourceId(subject.getMappingSourceId());
            po.setMappingStatus(subject.getMappingStatus());
            po.setMappingReason(subject.getMappingReason());
            po.setMappingConfidence(subject.getMappingConfidence() == null ? null : java.math.BigDecimal.valueOf(subject.getMappingConfidence()));
            po.setRawValuesJson(writeJson(subject.getRawValues()));
            poList.add(po);
        }
        subjectRepository.insertBatchSomeColumn(poList);
    }

    private void saveMetrics(Long valuationId, Long fileId, List<MetricRecord> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        List<DwdExternalValuationStandardMetricPO> poList = new ArrayList<>(metrics.size());
        for (MetricRecord metric : metrics) {
            DwdExternalValuationStandardMetricPO po = new DwdExternalValuationStandardMetricPO();
            po.setValuationId(valuationId);
            po.setFileId(fileId);
            po.setSheetName(metric.getSheetName());
            po.setRowDataNumber(metric.getRowDataNumber());
            po.setMetricName(metric.getMetricName());
            po.setMetricType(metric.getMetricType());
            po.setMetricCode(metric.getStandardCode());
            po.setMetricStandardName(metric.getStandardName());
            po.setStandardValueText(metric.getStandardValueText());
            po.setStandardValueNum(metric.getStandardValueNumber());
            po.setStandardValueUnit(metric.getStandardValueUnit());
            po.setStandardValuesJson(writeJson(metric.getStandardValues()));
            po.setMappingRuleId(metric.getMappingRuleId());
            po.setMappingSourceId(metric.getMappingSourceId());
            po.setMappingStatus(metric.getMappingStatus());
            po.setMappingReason(metric.getMappingReason());
            po.setMappingConfidence(metric.getMappingConfidence() == null ? null : java.math.BigDecimal.valueOf(metric.getMappingConfidence()));
            po.setRawValuesJson(writeJson(metric.getRawValues()));
            poList.add(po);
        }
        metricRepository.insertBatchSomeColumn(poList);
    }

    private List<SubjectRecord> loadSubjects(List<DwdExternalValuationStandardSubjectPO> poList) {
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        List<SubjectRecord> result = new ArrayList<>(poList.size());
        for (DwdExternalValuationStandardSubjectPO po : poList) {
            result.add(SubjectRecord.builder()
                    .sheetName(po.getSheetName())
                    .rowDataNumber(po.getRowDataNumber())
                    .subjectCode(po.getSubjectCode())
                    .subjectName(po.getSubjectName())
                    .level(po.getLevel())
                    .parentCode(po.getParentCode())
                    .rootCode(po.getRootCode())
                    .segmentCount(po.getSegmentCount())
                    .pathCodes(readJson(po.getPathCodesJson(), STRING_LIST_TYPE))
                    .leaf(po.getLeaf())
                    .standardCode(po.getStandardCode())
                    .standardName(po.getStandardName())
                    .standardValues(readJson(po.getStandardValuesJson(), MAP_TYPE))
                    .mappingRuleId(po.getMappingRuleId())
                    .mappingSourceId(po.getMappingSourceId())
                    .mappingStatus(po.getMappingStatus())
                    .mappingReason(po.getMappingReason())
                    .mappingConfidence(po.getMappingConfidence() == null ? null : po.getMappingConfidence().doubleValue())
                    .rawValues(readJson(po.getRawValuesJson(), new TypeReference<List<Object>>() {
                    }))
                    .build());
        }
        return result;
    }

    private List<MetricRecord> loadMetrics(List<DwdExternalValuationStandardMetricPO> poList) {
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        List<MetricRecord> result = new ArrayList<>(poList.size());
        for (DwdExternalValuationStandardMetricPO po : poList) {
            result.add(MetricRecord.builder()
                    .sheetName(po.getSheetName())
                    .rowDataNumber(po.getRowDataNumber())
                    .metricName(po.getMetricName())
                    .metricType(po.getMetricType())
                    .value(po.getStandardValueText())
                    .standardCode(po.getMetricCode())
                    .standardName(po.getMetricStandardName())
                    .standardValueText(po.getStandardValueText())
                    .standardValueNumber(po.getStandardValueNum())
                    .standardValueUnit(po.getStandardValueUnit())
                    .standardValues(readJson(po.getStandardValuesJson(), MAP_TYPE))
                    .mappingRuleId(po.getMappingRuleId())
                    .mappingSourceId(po.getMappingSourceId())
                    .mappingStatus(po.getMappingStatus())
                    .mappingReason(po.getMappingReason())
                    .mappingConfidence(po.getMappingConfidence() == null ? null : po.getMappingConfidence().doubleValue())
                    .rawValues(readJson(po.getRawValuesJson(), MAP_TYPE))
                    .build());
        }
        return result;
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("标准化结果序列化失败", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("标准化结果反序列化失败", exception);
        }
    }
}
