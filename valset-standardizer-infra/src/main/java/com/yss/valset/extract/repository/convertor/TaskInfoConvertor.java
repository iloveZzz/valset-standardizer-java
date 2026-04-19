package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.extract.repository.entity.TaskInfoPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskInfoConvertor {

    TaskInfoPO toPO(TaskInfo taskInfo);

    TaskInfo toDomain(TaskInfoPO po);
}
