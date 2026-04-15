package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.common.exception.TaskNotFoundException;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.model.TaskStatus;
import com.yss.subjectmatch.domain.model.TaskType;
import com.yss.subjectmatch.extract.repository.mapper.TaskInfoRepository;
import com.yss.subjectmatch.extract.repository.convertor.TaskInfoConvertor;
import com.yss.subjectmatch.extract.repository.entity.TaskInfoPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis 支持的任务网关。
 */
@Repository
@RequiredArgsConstructor
public class TaskGatewayImpl implements TaskGateway {

    private final TaskInfoRepository taskInfoRepository;
    private final TaskInfoConvertor taskInfoConvertor;

    /**
     * 保留任务并返回其生成的 id。
     */
    @Override
    public Long save(TaskInfo taskInfo) {
        TaskInfoPO po = taskInfoConvertor.toPO(taskInfo);
        taskInfoRepository.insert(po);
        taskInfo.setTaskId(po.getTaskId());
        return po.getTaskId();
    }

    /**
     * 通过id加载任务。
     */
    @Override
    public TaskInfo findById(Long taskId) {
        TaskInfoPO po = taskInfoRepository.selectById(taskId);
        if (po == null) {
            throw new TaskNotFoundException(taskId);
        }
        return taskInfoConvertor.toDomain(po);
    }

    /**
     * 按任务类型和业务密钥查找最新的成功任务。
     */
    @Override
    public TaskInfo findLatestSuccessfulTask(TaskType taskType, String businessKey) {
        if (taskType == null || businessKey == null || businessKey.isBlank()) {
            return null;
        }
        List<TaskInfoPO> poList = taskInfoRepository.selectList(
                Wrappers.lambdaQuery(TaskInfoPO.class)
                        .eq(TaskInfoPO::getTaskType, taskType.name())
                        .eq(TaskInfoPO::getTaskStatus, TaskStatus.SUCCESS.name())
                        .eq(TaskInfoPO::getBusinessKey, businessKey)
                        .orderByDesc(TaskInfoPO::getTaskId)
        );
        TaskInfoPO po = poList == null || poList.isEmpty() ? null : poList.get(0);
        return po == null ? null : taskInfoConvertor.toDomain(po);
    }

    /**
     * 如果任务仍可调度，则将其标记为正在运行。
     */
    @Override
    public boolean markRunning(Long taskId, String taskStage, LocalDateTime taskStartTime) {
        return taskInfoRepository.update(
                null,
                Wrappers.lambdaUpdate(TaskInfoPO.class)
                        .eq(TaskInfoPO::getTaskId, taskId)
                        .in(
                                TaskInfoPO::getTaskStatus,
                                TaskStatus.PENDING.name(),
                                TaskStatus.SCHEDULED.name(),
                                TaskStatus.RETRYING.name()
                        )
                        .set(TaskInfoPO::getTaskStatus, TaskStatus.RUNNING.name())
                        .set(taskStage != null, TaskInfoPO::getTaskStage, taskStage)
                        .set(taskStartTime != null, TaskInfoPO::getTaskStartTime, taskStartTime)
        ) > 0;
    }

    /**
     * 更新任务阶段耗时。
     */
    @Override
    public void updateTaskTimings(Long taskId, Long parseTaskTimeMs, Long standardizeTimeMs, Long matchStandardSubjectTimeMs) {
        taskInfoRepository.update(
                null,
                Wrappers.lambdaUpdate(TaskInfoPO.class)
                        .eq(TaskInfoPO::getTaskId, taskId)
                        .set(parseTaskTimeMs != null, TaskInfoPO::getParseTaskTimeMs, parseTaskTimeMs)
                        .set(standardizeTimeMs != null, TaskInfoPO::getStandardizeTimeMs, standardizeTimeMs)
                        .set(matchStandardSubjectTimeMs != null, TaskInfoPO::getMatchStandardSubjectTimeMs, matchStandardSubjectTimeMs)
        );
    }

    /**
     * 将任务标记为成功并存储其结果负载。
     */
    @Override
    public void markSuccess(Long taskId, String resultPayload) {
        taskInfoRepository.update(
                null,
                Wrappers.lambdaUpdate(TaskInfoPO.class)
                        .eq(TaskInfoPO::getTaskId, taskId)
                        .set(TaskInfoPO::getTaskStatus, TaskStatus.SUCCESS.name())
                        .set(TaskInfoPO::getResultPayload, resultPayload)
        );
    }

    /**
     * 将任务标记为失败并存储错误消息。
     */
    @Override
    public void markFailed(Long taskId, String errorMessage) {
        taskInfoRepository.update(
                null,
                Wrappers.lambdaUpdate(TaskInfoPO.class)
                        .eq(TaskInfoPO::getTaskId, taskId)
                        .set(TaskInfoPO::getTaskStatus, TaskStatus.FAILED.name())
                        .set(TaskInfoPO::getResultPayload, errorMessage)
        );
    }
}
