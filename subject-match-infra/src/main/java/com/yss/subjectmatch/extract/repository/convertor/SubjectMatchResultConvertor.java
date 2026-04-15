package com.yss.subjectmatch.extract.repository.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.ConfidenceLevel;
import com.yss.subjectmatch.domain.model.MatchCandidate;
import com.yss.subjectmatch.domain.model.SubjectMatchResult;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchResultPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = JsonStringMapper.class)
public interface SubjectMatchResultConvertor {

    @Mapping(target = "topCandidatesJson", source = "result.topCandidates")
    SubjectMatchResultPO toPO(Long taskId, Long fileId, SubjectMatchResult result);

    @Mapping(target = "topCandidates", source = "topCandidatesJson")
    SubjectMatchResult toDomain(SubjectMatchResultPO po);
}
