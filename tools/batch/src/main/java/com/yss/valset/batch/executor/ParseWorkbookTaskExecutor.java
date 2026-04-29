package com.yss.valset.batch.executor;

import com.yss.valset.analysis.application.port.ParseExecutionUseCase;
import com.yss.valset.batch.task.TaskExecutor;
import com.yss.valset.domain.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 解析工作簿任务的执行器。
 */
@Slf4j
@Component
public class ParseWorkbookTaskExecutor implements TaskExecutor {

    private final ParseExecutionUseCase parseExecutionUseCase;

    public ParseWorkbookTaskExecutor(ParseExecutionUseCase parseExecutionUseCase) {
        this.parseExecutionUseCase = parseExecutionUseCase;
    }

    /**
     * 支持解析工作簿任务。
     */
    @Override
    public TaskType support() {
        return TaskType.PARSE_WORKBOOK;
    }

    /**
     * 执行解析工作流程。
     */
    @Override
    public void execute(Long taskId) {
        log.info("开始分派工作簿解析任务，taskId={}", taskId);
        parseExecutionUseCase.execute(taskId);
        log.info("工作簿解析任务分派完成，taskId={}", taskId);
    }
}
