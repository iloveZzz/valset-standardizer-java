package com.yss.subjectmatch.extract.repository.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.extract.repository.entity.ParsedValuationDataPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = JsonStringMapper.class)
public interface ParsedValuationDataConvertor {

    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "fileId", source = "fileId")
    @Mapping(target = "basicInfoJson", source = "parsedValuationData.basicInfo")
    @Mapping(target = "headersJson", source = "parsedValuationData.headers")
    @Mapping(target = "headerDetailsJson", source = "parsedValuationData.headerDetails")
    @Mapping(target = "headerColumnsJson", source = "parsedValuationData.headerColumns")
    @Mapping(target = "subjectsJson", source = "parsedValuationData.subjects")
    @Mapping(target = "metricsJson", source = "parsedValuationData.metrics")
    ParsedValuationDataPO toPO(Long taskId, Long fileId, ParsedValuationData parsedValuationData);

    @Mapping(target = "basicInfo", source = "basicInfoJson")
    @Mapping(target = "headers", source = "headersJson")
    @Mapping(target = "headerDetails", source = "headerDetailsJson")
    @Mapping(target = "headerColumns", source = "headerColumnsJson")
    @Mapping(target = "subjects", source = "subjectsJson")
    @Mapping(target = "metrics", source = "metricsJson")
    ParsedValuationData toDomain(ParsedValuationDataPO po);
}
