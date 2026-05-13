package com.yss.valset.batch.executor;

import com.yss.valset.application.port.EvaluateMappingExecutionUseCase;
import com.yss.valset.batch.task.TaskExecutor;
import com.yss.valset.domain.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 离线评估任务的执行器。
 */
@Slf4j
@Component
public class EvaluateMappingTaskExecutor implements TaskExecutor {

    private final EvaluateMappingExecutionUseCase evaluateMappingExecutionUseCase;

    public EvaluateMappingTaskExecutor(EvaluateMappingExecutionUseCase evaluateMappingExecutionUseCase) {
        this.evaluateMappingExecutionUseCase = evaluateMappingExecutionUseCase;
    }

    /**
     * 支持评估任务。
     */
    @Override
    public TaskType support() {
        return TaskType.EVALUATE_MAPPING;
    }

    /**
     * 执行评估工作流程。
     */
    @Override
    public void execute(Long taskId) {
        log.info("开始分派映射评估任务，taskId={}", taskId);
        evaluateMappingExecutionUseCase.execute(taskId);
        log.info("映射评估任务分派完成，taskId={}", taskId);
    }
}
