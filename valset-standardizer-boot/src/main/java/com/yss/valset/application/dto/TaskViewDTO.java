package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 读取任务查询端点返回的模型。
 */
@Data
@Builder
public class TaskViewDTO {
    /**
     * 唯一的任务标识符。
     */
    private Long taskId;
    /**
     * 任务类型名称。
     */
    private String taskType;
    /**
     * 任务阶段标志。
     */
    private String taskStage;
    /**
     * 当前任务状态。
     */
    private String taskStatus;
    /**
     * 创建任务时使用的业务密钥。
     */
    private String businessKey;
    /**
     * 原始输入负载存储在任务记录中。
     */
    private String inputPayload;
    /**
     * 当输入负载为有效的JSON时，已解析输入负载。
     */
    private Map<String, Object> inputData;
    /**
     * 原始结果负载存储在任务记录中。
     */
    private String resultPayload;
    /**
     * 当结果有效负载为有效JSON时，解析结果负载。
     */
    private Map<String, Object> resultData;
    /**
     * 任务失败时的可读原因，优先取结果负载里的 errorMessage。
     */
    private String errorMessage;
    /**
     * 任务失败时的错误分类码，供前端按固定状态展示。
     */
    private String errorCode;
    /**
     * 提取任务的总行数。
     */
    private Long rowCount;
    /**
     * 提取任务的源文件大小，单位字节。
     */
    private Long fileSizeBytes;
    /**
     * 提取任务耗时，单位毫秒。
     */
    private Long durationMs;
    /**
     * 任务开始时间。
     */
    private LocalDateTime taskStartTime;
    /**
     * 文件解析耗时，单位毫秒。
     */
    private Long parseTaskTimeMs;
    /**
     * 结构标准化耗时，单位毫秒。
     */
    private Long standardizeTimeMs;
    /**
     * 匹配标准科目耗时，单位毫秒。
     */
    private Long matchStandardSubjectTimeMs;
}
