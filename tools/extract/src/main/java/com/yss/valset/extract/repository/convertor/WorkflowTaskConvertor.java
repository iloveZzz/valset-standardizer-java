package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.extract.repository.entity.WorkflowTaskPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkflowTaskConvertor {

    WorkflowTaskPO toPO(WorkflowTask workflowTask);

    WorkflowTask toDomain(WorkflowTaskPO po);
}
