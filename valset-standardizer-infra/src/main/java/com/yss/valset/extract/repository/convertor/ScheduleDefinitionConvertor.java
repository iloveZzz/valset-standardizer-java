package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.ScheduleDefinition;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.extract.repository.entity.ScheduleDefinitionPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduleDefinitionConvertor {

    ScheduleDefinitionPO toPO(ScheduleDefinition scheduleDefinition);

    ScheduleDefinition toDomain(ScheduleDefinitionPO po);
}
