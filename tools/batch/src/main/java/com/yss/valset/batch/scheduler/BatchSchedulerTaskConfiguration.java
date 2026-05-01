package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.yss.valset.batch.dispatcher.TaskDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 批处理调度任务定义。
 */
@Configuration
public class BatchSchedulerTaskConfiguration {

    @Bean
    public OneTimeTask<BatchDispatchTaskData> batchDispatchOnceTask(TaskDispatcher taskDispatcher) {
        return Tasks.oneTime(BatchSchedulerTasks.DISPATCH_ONCE_TASK)
                .onFailure((executionComplete, executionOperations) -> executionOperations.remove())
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> taskDispatcher.dispatchTask(taskInstance.getData().taskId()));
    }
}
