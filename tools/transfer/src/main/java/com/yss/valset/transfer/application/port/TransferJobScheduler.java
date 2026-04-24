package com.yss.valset.transfer.application.port;

/**
 * 文件收发分拣作业调度器。
 */
public interface TransferJobScheduler {

    String triggerIngest(String sourceId, String sourceType, String sourceCode, java.util.Map<String, Object> parameters, String ingestLockToken);

    void scheduleIngestCron(String sourceId, String sourceType, String sourceCode, java.util.Map<String, Object> parameters, String cronExpression);

    void unscheduleIngest(String sourceId);

    void triggerRoute(String transferId);

    void triggerDeliver(String routeId, String transferId);

    void scheduleDeliverRetry(String routeId, String transferId, int retryCount, int delaySeconds);
}
