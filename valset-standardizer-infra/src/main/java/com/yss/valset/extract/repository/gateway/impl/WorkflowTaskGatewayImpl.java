package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.common.exception.TaskNotFoundException;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.extract.repository.mapper.WorkflowTaskRepository;
import com.yss.valset.extract.repository.convertor.WorkflowTaskConvertor;
import com.yss.valset.extract.repository.entity.WorkflowTaskPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis 支持的工作流任务网关。
 */
@Repository
@RequiredArgsConstructor
public class WorkflowTaskGatewayImpl implements WorkflowTaskGateway {

    private final WorkflowTaskRepository workflowTaskRepository;
    private final WorkflowTaskConvertor workflowTaskConvertor;

    /**
     * 保留任务并返回其生成的 id。
     */
    @Override
    public Long save(WorkflowTask workflowTask) {
        WorkflowTaskPO po = workflowTaskConvertor.toPO(workflowTask);
        workflowTaskRepository.insert(po);
        workflowTask.setTaskId(po.getTaskId());
        return po.getTaskId();
    }

    /**
     * 通过id加载任务。
     */
    @Override
    public WorkflowTask findById(Long taskId) {
        WorkflowTaskPO po = workflowTaskRepository.selectById(taskId);
        if (po == null) {
            throw new TaskNotFoundException(taskId);
        }
        return workflowTaskConvertor.toDomain(po);
    }

    /**
     * 按任务类型和业务密钥查找最新的成功任务。
     */
    @Override
    public WorkflowTask findLatestSuccessfulTask(TaskType taskType, String businessKey) {
        if (taskType == null || businessKey == null || businessKey.isBlank()) {
            return null;
        }
        List<WorkflowTaskPO> poList = workflowTaskRepository.selectList(
                Wrappers.lambdaQuery(WorkflowTaskPO.class)
                        .eq(WorkflowTaskPO::getTaskType, taskType.name())
                        .eq(WorkflowTaskPO::getTaskStatus, TaskStatus.SUCCESS.name())
                        .eq(WorkflowTaskPO::getBusinessKey, businessKey)
                        .orderByDesc(WorkflowTaskPO::getTaskId)
        );
        WorkflowTaskPO po = poList == null || poList.isEmpty() ? null : poList.get(0);
        return po == null ? null : workflowTaskConvertor.toDomain(po);
    }

    /**
     * 如果任务仍可调度，则将其标记为正在运行。
     */
    @Override
    public boolean markRunning(Long taskId, String taskStage, LocalDateTime taskStartTime) {
        return workflowTaskRepository.update(
                null,
                Wrappers.lambdaUpdate(WorkflowTaskPO.class)
                        .eq(WorkflowTaskPO::getTaskId, taskId)
                        .in(
                                WorkflowTaskPO::getTaskStatus,
                                TaskStatus.PENDING.name(),
                                TaskStatus.SCHEDULED.name(),
                                TaskStatus.RETRYING.name()
                        )
                        .set(WorkflowTaskPO::getTaskStatus, TaskStatus.RUNNING.name())
                        .set(taskStage != null, WorkflowTaskPO::getTaskStage, taskStage)
                        .set(taskStartTime != null, WorkflowTaskPO::getTaskStartTime, taskStartTime)
        ) > 0;
    }

    /**
     * 更新任务阶段耗时。
     */
    @Override
    public void updateTaskTimings(Long taskId, Long parseTaskTimeMs, Long standardizeTimeMs, Long matchStandardSubjectTimeMs) {
        workflowTaskRepository.update(
                null,
                Wrappers.lambdaUpdate(WorkflowTaskPO.class)
                        .eq(WorkflowTaskPO::getTaskId, taskId)
                        .set(parseTaskTimeMs != null, WorkflowTaskPO::getParseTaskTimeMs, parseTaskTimeMs)
                        .set(standardizeTimeMs != null, WorkflowTaskPO::getStandardizeTimeMs, standardizeTimeMs)
                        .set(matchStandardSubjectTimeMs != null, WorkflowTaskPO::getMatchStandardSubjectTimeMs, matchStandardSubjectTimeMs)
        );
    }

    /**
     * 将任务标记为成功并存储其结果负载。
     */
    @Override
    public void markSuccess(Long taskId, String resultPayload) {
        workflowTaskRepository.update(
                null,
                Wrappers.lambdaUpdate(WorkflowTaskPO.class)
                        .eq(WorkflowTaskPO::getTaskId, taskId)
                        .set(WorkflowTaskPO::getTaskStatus, TaskStatus.SUCCESS.name())
                        .set(WorkflowTaskPO::getResultPayload, resultPayload)
        );
    }

    /**
     * 将任务标记为失败并存储错误消息。
     */
    @Override
    public void markFailed(Long taskId, String errorMessage) {
        workflowTaskRepository.update(
                null,
                Wrappers.lambdaUpdate(WorkflowTaskPO.class)
                        .eq(WorkflowTaskPO::getTaskId, taskId)
                        .set(WorkflowTaskPO::getTaskStatus, TaskStatus.FAILED.name())
                        .set(WorkflowTaskPO::getResultPayload, errorMessage)
        );
    }
}
