package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 保留计划定义。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDefinition {
    private Long scheduleId;
    private String scheduleName;
    private TaskType taskType;
    private String cronExpression;
    private Boolean enabled;
    private String schedulePayload;
    private LocalDateTime lastTriggerTime;
    private LocalDateTime nextTriggerTime;

    /**
     * 构建用于运行时任务创建的业务密钥。
     */
    public String buildRuntimeBusinessKey() {
        return taskType + ":" + scheduleId + ":" + System.currentTimeMillis();
    }
}
