package com.yss.valset.batch.executor;

import com.yss.valset.application.port.MatchExecutionUseCase;
import com.yss.valset.batch.task.TaskExecutor;
import com.yss.valset.domain.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 匹配任务的执行器。
 */
@Slf4j
@Component
public class MatchSubjectTaskExecutor implements TaskExecutor {

    private final MatchExecutionUseCase matchExecutionUseCase;

    public MatchSubjectTaskExecutor(MatchExecutionUseCase matchExecutionUseCase) {
        this.matchExecutionUseCase = matchExecutionUseCase;
    }

    /**
     * 支持匹配任务。
     */
    @Override
    public TaskType support() {
        return TaskType.MATCH_SUBJECT;
    }

    /**
     * 执行匹配工作流程。
     */
    @Override
    public void execute(Long taskId) {
        log.info("开始分派估值匹配任务，taskId={}", taskId);
        matchExecutionUseCase.execute(taskId);
        log.info("估值匹配任务分派完成，taskId={}", taskId);
    }
}
