package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.ScheduleDefinition;
import com.yss.subjectmatch.domain.model.TaskType;
import com.yss.subjectmatch.extract.repository.entity.ScheduleDefinitionPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduleDefinitionConvertor {

    ScheduleDefinitionPO toPO(ScheduleDefinition scheduleDefinition);

    ScheduleDefinition toDomain(ScheduleDefinitionPO po);
}
