package com.yss.subjectmatch.batch.job;

import com.yss.subjectmatch.batch.dispatcher.TaskDispatcher;
import org.quartz.*;
import org.springframework.stereotype.Component;

/**
 * Quartz 作业分派一次性任务或计划任务。
 */
@Component
@DisallowConcurrentExecution
public class DispatchJob implements Job {

    public static final String TASK_ID = "taskId";
    public static final String SCHEDULE_ID = "scheduleId";

    private final TaskDispatcher taskDispatcher;

    public DispatchJob(TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    /**
     * 解析作业数据图并调度相应的任务流程。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        if (dataMap.containsKey(TASK_ID)) {
            taskDispatcher.dispatchTask(dataMap.getLong(TASK_ID));
            return;
        }
        if (dataMap.containsKey(SCHEDULE_ID)) {
            taskDispatcher.dispatchSchedule(dataMap.getLong(SCHEDULE_ID));
            return;
        }
        throw new JobExecutionException("Neither taskId nor scheduleId found in job data");
    }
}
