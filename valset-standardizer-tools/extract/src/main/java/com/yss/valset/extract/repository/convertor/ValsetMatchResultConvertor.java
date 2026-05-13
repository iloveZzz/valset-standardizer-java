package com.yss.valset.extract.repository.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.ConfidenceLevel;
import com.yss.valset.domain.model.MatchCandidate;
import com.yss.valset.domain.model.ValsetMatchResult;
import com.yss.valset.extract.repository.entity.ValsetMatchResultPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = JsonStringMapper.class)
public interface ValsetMatchResultConvertor {

    @Mapping(target = "topCandidatesJson", source = "result.topCandidates")
    ValsetMatchResultPO toPO(Long taskId, Long fileId, ValsetMatchResult result);

    @Mapping(target = "topCandidates", source = "topCandidatesJson")
    ValsetMatchResult toDomain(ValsetMatchResultPO po);
}
