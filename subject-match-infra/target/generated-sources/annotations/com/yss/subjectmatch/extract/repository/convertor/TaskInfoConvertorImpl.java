package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.model.TaskStage;
import com.yss.subjectmatch.domain.model.TaskStatus;
import com.yss.subjectmatch.domain.model.TaskType;
import com.yss.subjectmatch.extract.repository.entity.TaskInfoPO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class TaskInfoConvertorImpl implements TaskInfoConvertor {

    @Override
    public TaskInfoPO toPO(TaskInfo taskInfo) {
        if ( taskInfo == null ) {
            return null;
        }

        TaskInfoPO taskInfoPO = new TaskInfoPO();

        taskInfoPO.setTaskId( taskInfo.getTaskId() );
        if ( taskInfo.getTaskType() != null ) {
            taskInfoPO.setTaskType( taskInfo.getTaskType().name() );
        }
        if ( taskInfo.getTaskStatus() != null ) {
            taskInfoPO.setTaskStatus( taskInfo.getTaskStatus().name() );
        }
        if ( taskInfo.getTaskStage() != null ) {
            taskInfoPO.setTaskStage( taskInfo.getTaskStage().name() );
        }
        taskInfoPO.setBusinessKey( taskInfo.getBusinessKey() );
        taskInfoPO.setFileId( taskInfo.getFileId() );
        taskInfoPO.setInputPayload( taskInfo.getInputPayload() );
        taskInfoPO.setResultPayload( taskInfo.getResultPayload() );
        taskInfoPO.setTaskStartTime( taskInfo.getTaskStartTime() );
        taskInfoPO.setParseTaskTimeMs( taskInfo.getParseTaskTimeMs() );
        taskInfoPO.setStandardizeTimeMs( taskInfo.getStandardizeTimeMs() );
        taskInfoPO.setMatchStandardSubjectTimeMs( taskInfo.getMatchStandardSubjectTimeMs() );

        return taskInfoPO;
    }

    @Override
    public TaskInfo toDomain(TaskInfoPO po) {
        if ( po == null ) {
            return null;
        }

        TaskInfo.TaskInfoBuilder taskInfo = TaskInfo.builder();

        taskInfo.taskId( po.getTaskId() );
        if ( po.getTaskType() != null ) {
            taskInfo.taskType( Enum.valueOf( TaskType.class, po.getTaskType() ) );
        }
        if ( po.getTaskStatus() != null ) {
            taskInfo.taskStatus( Enum.valueOf( TaskStatus.class, po.getTaskStatus() ) );
        }
        if ( po.getTaskStage() != null ) {
            taskInfo.taskStage( Enum.valueOf( TaskStage.class, po.getTaskStage() ) );
        }
        taskInfo.businessKey( po.getBusinessKey() );
        taskInfo.fileId( po.getFileId() );
        taskInfo.inputPayload( po.getInputPayload() );
        taskInfo.resultPayload( po.getResultPayload() );
        taskInfo.taskStartTime( po.getTaskStartTime() );
        taskInfo.parseTaskTimeMs( po.getParseTaskTimeMs() );
        taskInfo.standardizeTimeMs( po.getStandardizeTimeMs() );
        taskInfo.matchStandardSubjectTimeMs( po.getMatchStandardSubjectTimeMs() );

        return taskInfo.build();
    }
}
