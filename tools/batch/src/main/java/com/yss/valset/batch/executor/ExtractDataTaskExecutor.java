package com.yss.valset.batch.executor;

import com.yss.valset.application.port.ExtractDataExecutionUseCase;
import com.yss.valset.batch.task.TaskExecutor;
import com.yss.valset.domain.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 原始数据提取任务执行器。
 */
@Slf4j
@Component
public class ExtractDataTaskExecutor implements TaskExecutor {

    private final ExtractDataExecutionUseCase extractDataExecutionUseCase;

    public ExtractDataTaskExecutor(ExtractDataExecutionUseCase extractDataExecutionUseCase) {
        this.extractDataExecutionUseCase = extractDataExecutionUseCase;
    }

    @Override
    public TaskType support() {
        return TaskType.EXTRACT_DATA;
    }

    @Override
    public void execute(Long taskId) {
        log.info("开始分派原始数据提取任务，taskId={}", taskId);
        extractDataExecutionUseCase.execute(taskId);
        log.info("原始数据提取任务分派完成，taskId={}", taskId);
    }
}
