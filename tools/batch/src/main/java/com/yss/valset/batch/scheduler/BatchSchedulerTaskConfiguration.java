package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.yss.valset.batch.dispatcher.TaskDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 批处理调度任务定义。
 */
@Configuration
public class BatchSchedulerTaskConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BatchSchedulerTaskConfiguration.class);

    @Bean
    public OneTimeTask<BatchDispatchTaskData> batchDispatchOnceTask(TaskDispatcher taskDispatcher) {
        return Tasks.oneTime(BatchSchedulerTasks.DISPATCH_ONCE_TASK)
                .onFailure((executionComplete, executionOperations) -> executionOperations.remove())
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> taskDispatcher.dispatchTask(taskInstance.getData().taskId()));
    }

    @Bean
    public RecurringTaskWithPersistentSchedule<BatchScheduleTaskData> batchDispatchCronTask(TaskDispatcher taskDispatcher) {
        return Tasks.recurringWithPersistentSchedule(BatchSchedulerTasks.DISPATCH_CRON_TASK)
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> {
                    try {
                        taskDispatcher.dispatchSchedule(taskInstance.getData().scheduleId());
                    } catch (Exception exception) {
                        log.warn("计划任务执行失败，scheduleId={}", taskInstance.getData().scheduleId(), exception);
                    }
                });
    }
}
