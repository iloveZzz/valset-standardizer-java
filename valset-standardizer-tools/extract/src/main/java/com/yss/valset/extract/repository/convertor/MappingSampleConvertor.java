package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.MappingSample;
import com.yss.valset.extract.repository.entity.MappingSamplePO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 映射样例领域对象与持久化对象转换器。
 */
@Mapper(componentModel = "spring")
public interface MappingSampleConvertor {
    MappingSamplePO toPO(MappingSample mappingSample);

    MappingSample toDomain(MappingSamplePO po);

    List<MappingSamplePO> toPO(List<MappingSample> mappingSamples);

    List<MappingSample> toDomain(List<MappingSamplePO> poList);
}
