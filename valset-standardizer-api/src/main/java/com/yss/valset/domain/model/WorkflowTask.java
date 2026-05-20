package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 估值标准化工作流任务记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTask {
    /** 任务ID */
    private Long taskId;
    /** 任务类型 */
    private TaskType taskType;
    /** 任务状态 */
    private TaskStatus taskStatus;
    /** 任务阶段 */
    private TaskStage taskStage;
    /** 业务键 */
    private String businessKey;
    /** 文件ID */
    private Long fileId;
    /** 输入参数 */
    private String inputPayload;
    /** 结果参数 */
    private String resultPayload;
    /** 任务开始时间 */
    private LocalDateTime taskStartTime;
    /** 解析任务耗时(ms) */
    private Long parseTaskTimeMs;
    /** 标准化耗时(ms) */
    private Long standardizeTimeMs;
    /** 匹配标准主题耗时(ms) */
    private Long matchStandardSubjectTimeMs;
}
