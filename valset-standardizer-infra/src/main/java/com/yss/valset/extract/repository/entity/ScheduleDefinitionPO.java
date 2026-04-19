package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.cloud.sankuai.GenerationTypeSeq;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@TableName("t_subject_match_schedule")
public class ScheduleDefinitionPO {
    @Id
    @TableId(value = "schedule_id", type = IdType.ASSIGN_ID)
    private Long scheduleId;

    @TableField("schedule_name")
    private String scheduleName;

    @TableField("task_type")
    private String taskType;

    @TableField("cron_expression")
    private String cronExpression;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("schedule_payload")
    private String schedulePayload;

    @TableField("last_trigger_time")
    private LocalDateTime lastTriggerTime;

    @TableField("next_trigger_time")
    private LocalDateTime nextTriggerTime;
}
