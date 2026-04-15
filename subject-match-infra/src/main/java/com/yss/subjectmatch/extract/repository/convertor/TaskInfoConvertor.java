package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.extract.repository.entity.TaskInfoPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskInfoConvertor {

    TaskInfoPO toPO(TaskInfo taskInfo);

    TaskInfo toDomain(TaskInfoPO po);
}
