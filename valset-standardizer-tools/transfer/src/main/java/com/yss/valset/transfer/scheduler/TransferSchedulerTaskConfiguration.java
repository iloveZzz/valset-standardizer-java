package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverResponse;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import com.yss.valset.transfer.application.service.TransferObjectManagementAppService;
import com.yss.valset.transfer.application.port.TransferRunLogMaintenanceUseCase;
import com.yss.valset.transfer.scheduler.task.TransferDeliverTaskData;
import com.yss.valset.transfer.scheduler.task.TransferIngestScheduledTaskData;
import com.yss.valset.transfer.scheduler.task.TransferIngestTaskData;
import com.yss.valset.transfer.scheduler.task.TransferObjectRedeliverScheduledTaskData;
import com.yss.valset.transfer.scheduler.task.TransferObjectRedeliverTaskData;
import com.yss.valset.transfer.scheduler.task.TransferRouteTaskData;
import com.yss.valset.transfer.scheduler.task.TransferRunLogCleanupScheduledTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件分拣任务定义。
 */
@Configuration
public class TransferSchedulerTaskConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TransferSchedulerTaskConfiguration.class);

    @Bean
    public RecurringTaskWithPersistentSchedule<TransferIngestScheduledTaskData> transferIngestCronTask(TransferProcessUseCase transferProcessUseCase) {
        return Tasks.recurringWithPersistentSchedule(TransferSchedulerTasks.INGEST_CRON_TASK)
                .execute((taskInstance, executionContext) -> transferProcessUseCase.ingest(
                        taskInstance.getData().payload().toCommand()
                ));
    }

    @Bean
    public OneTimeTask<TransferIngestTaskData> transferIngestOnceTask(TransferProcessUseCase transferProcessUseCase) {
        return Tasks.oneTime(TransferSchedulerTasks.INGEST_ONCE_TASK)
                .onFailure((executionComplete, executionOperations) -> executionOperations.remove())
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> transferProcessUseCase.ingest(taskInstance.getData().toCommand()));
    }

    @Bean
    public OneTimeTask<TransferRouteTaskData> transferRouteTask(TransferProcessUseCase transferProcessUseCase) {
        return Tasks.oneTime(TransferSchedulerTasks.ROUTE_TASK)
                .onFailure((executionComplete, executionOperations) -> executionOperations.remove())
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> transferProcessUseCase.route(taskInstance.getData().transferId()));
    }

    @Bean
    public OneTimeTask<TransferDeliverTaskData> transferDeliverTask(TransferProcessUseCase transferProcessUseCase) {
        return Tasks.oneTime(TransferSchedulerTasks.DELIVER_TASK)
                .onFailure((executionComplete, executionOperations) -> executionOperations.remove())
                .onDeadExecutionRevive()
                .execute((taskInstance, executionContext) -> transferProcessUseCase.deliver(
                        taskInstance.getData().routeId(),
                        taskInstance.getData().transferId(),
                        taskInstance.getData().retryCount()
                ));
    }

    @Bean
    public RecurringTaskWithPersistentSchedule<TransferObjectRedeliverScheduledTaskData> transferObjectRedeliverTask(
            TransferObjectManagementAppService transferObjectManagementAppService) {
        return Tasks.recurringWithPersistentSchedule(TransferSchedulerTasks.OBJECT_REDELIVER_TASK)
                .execute((taskInstance, executionContext) -> {
                    try {
                        TransferObjectRedeliverResponse response = transferObjectManagementAppService.redeliverIdentifiedUndelivered();
                        log.info("分拣对象定时重投递完成，requestedCount={}，successCount={}，failureCount={}，skippedCount={}",
                                response.getRequestedCount(),
                                response.getSuccessCount(),
                                response.getFailureCount(),
                                response.getSkippedCount());
                    } catch (RuntimeException exception) {
                        log.warn("分拣对象定时重投递失败", exception);
                    }
                });
    }

    @Bean
    public RecurringTaskWithPersistentSchedule<TransferRunLogCleanupScheduledTaskData> transferRunLogCleanupTask(
            TransferRunLogMaintenanceUseCase transferRunLogMaintenanceUseCase) {
        return Tasks.recurringWithPersistentSchedule(TransferSchedulerTasks.RUN_LOG_CLEANUP_TASK)
                .execute((taskInstance, executionContext) -> {
                    try {
                        transferRunLogMaintenanceUseCase.cleanupYesterdayLogs();
                    } catch (RuntimeException exception) {
                        log.warn("文件收发运行日志清理失败", exception);
                    }
                });
    }
}
