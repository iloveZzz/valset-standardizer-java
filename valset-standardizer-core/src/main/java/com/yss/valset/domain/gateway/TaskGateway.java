package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskType;

import java.time.LocalDateTime;

/**
 * 任务持久性和状态转换的网关。
 */
public interface TaskGateway {
    /**
     * 坚持新任务。
     */
    Long save(TaskInfo taskInfo);

    /**
     * 通过id加载任务。
     */
    TaskInfo findById(Long taskId);

    /**
     * 按任务类型和业务密钥查找最新的成功任务。
     */
    TaskInfo findLatestSuccessfulTask(TaskType taskType, String businessKey);

    /**
     * 如果任务仍可调度，则将其标记为正在运行。
     */
    default boolean markRunning(Long taskId) {
        return markRunning(taskId, null, null);
    }

    /**
     * 将任务标记为运行中，并记录任务阶段与开始时间。
     */
    boolean markRunning(Long taskId, String taskStage, LocalDateTime taskStartTime);

    /**
     * 更新任务阶段耗时。
     */
    void updateTaskTimings(Long taskId, Long parseTaskTimeMs, Long standardizeTimeMs, Long matchStandardSubjectTimeMs);

    /**
     * 将任务标记为成功并保留结果负载。
     */
    void markSuccess(Long taskId, String resultPayload);

    /**
     * 将任务标记为失败并保留错误消息。
     */
    void markFailed(Long taskId, String errorMessage);
}
