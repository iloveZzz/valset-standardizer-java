package com.yss.subjectmatch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久的任务记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskInfo {
    private Long taskId;
    private TaskType taskType;
    private TaskStatus taskStatus;
    private TaskStage taskStage;
    private String businessKey;
    private Long fileId;
    private String inputPayload;
    private String resultPayload;
    private LocalDateTime taskStartTime;
    private Long parseTaskTimeMs;
    private Long standardizeTimeMs;
    private Long matchStandardSubjectTimeMs;
}
