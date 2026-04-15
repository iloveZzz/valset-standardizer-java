package com.yss.subjectmatch.batch.scheduler;

import com.yss.subjectmatch.domain.model.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCommand {
    private String scheduleKey;
    private TaskType taskType;
    private String cronExpression;
    private Long taskId;
    private Long scheduleId;
    private Map<String, Object> payload;
}
