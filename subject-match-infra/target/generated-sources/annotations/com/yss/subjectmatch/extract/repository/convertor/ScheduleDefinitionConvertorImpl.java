package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.ScheduleDefinition;
import com.yss.subjectmatch.domain.model.TaskType;
import com.yss.subjectmatch.extract.repository.entity.ScheduleDefinitionPO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ScheduleDefinitionConvertorImpl implements ScheduleDefinitionConvertor {

    @Override
    public ScheduleDefinitionPO toPO(ScheduleDefinition scheduleDefinition) {
        if ( scheduleDefinition == null ) {
            return null;
        }

        ScheduleDefinitionPO scheduleDefinitionPO = new ScheduleDefinitionPO();

        scheduleDefinitionPO.setScheduleId( scheduleDefinition.getScheduleId() );
        scheduleDefinitionPO.setScheduleName( scheduleDefinition.getScheduleName() );
        if ( scheduleDefinition.getTaskType() != null ) {
            scheduleDefinitionPO.setTaskType( scheduleDefinition.getTaskType().name() );
        }
        scheduleDefinitionPO.setCronExpression( scheduleDefinition.getCronExpression() );
        scheduleDefinitionPO.setEnabled( scheduleDefinition.getEnabled() );
        scheduleDefinitionPO.setSchedulePayload( scheduleDefinition.getSchedulePayload() );
        scheduleDefinitionPO.setLastTriggerTime( scheduleDefinition.getLastTriggerTime() );
        scheduleDefinitionPO.setNextTriggerTime( scheduleDefinition.getNextTriggerTime() );

        return scheduleDefinitionPO;
    }

    @Override
    public ScheduleDefinition toDomain(ScheduleDefinitionPO po) {
        if ( po == null ) {
            return null;
        }

        ScheduleDefinition.ScheduleDefinitionBuilder scheduleDefinition = ScheduleDefinition.builder();

        scheduleDefinition.scheduleId( po.getScheduleId() );
        scheduleDefinition.scheduleName( po.getScheduleName() );
        if ( po.getTaskType() != null ) {
            scheduleDefinition.taskType( Enum.valueOf( TaskType.class, po.getTaskType() ) );
        }
        scheduleDefinition.cronExpression( po.getCronExpression() );
        scheduleDefinition.enabled( po.getEnabled() );
        scheduleDefinition.schedulePayload( po.getSchedulePayload() );
        scheduleDefinition.lastTriggerTime( po.getLastTriggerTime() );
        scheduleDefinition.nextTriggerTime( po.getNextTriggerTime() );

        return scheduleDefinition.build();
    }
}
