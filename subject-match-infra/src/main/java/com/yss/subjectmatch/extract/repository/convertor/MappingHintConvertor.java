package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.MappingHint;
import com.yss.subjectmatch.extract.repository.entity.MappingHintPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MappingHintConvertor {
    MappingHintPO toPO(MappingHint mappingHint);

    MappingHint toDomain(MappingHintPO po);
}
