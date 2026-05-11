package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.MappingHint;
import com.yss.valset.extract.repository.entity.MappingHintPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MappingHintConvertor {
    MappingHintPO toPO(MappingHint mappingHint);

    MappingHint toDomain(MappingHintPO po);
}
