package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@TableName("t_subject_match_task")
public class TaskInfoPO {
    @Id
    @TableId(value = "task_id", type = IdType.ASSIGN_ID)
    private Long taskId;

    @TableField("task_type")
    private String taskType;

    @TableField("task_status")
    private String taskStatus;

    @TableField("task_stage")
    private String taskStage;

    @TableField("business_key")
    private String businessKey;

    @TableField("file_id")
    private Long fileId;

    @TableField("input_payload")
    private String inputPayload;

    @TableField("result_payload")
    private String resultPayload;

    @TableField("task_start_time")
    private LocalDateTime taskStartTime;

    @TableField("parse_task_time_ms")
    private Long parseTaskTimeMs;

    @TableField("standardize_time_ms")
    private Long standardizeTimeMs;

    @TableField("match_standard_subject_time_ms")
    private Long matchStandardSubjectTimeMs;
}
