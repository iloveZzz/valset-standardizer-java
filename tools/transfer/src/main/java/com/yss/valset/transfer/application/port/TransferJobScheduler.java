package com.yss.valset.transfer.application.port;

/**
 * 文件收发分拣作业调度器。
 */
public interface TransferJobScheduler {

    void triggerIngest(Long sourceId, String sourceType, String sourceCode, java.util.Map<String, Object> parameters);

    void triggerRoute(Long transferId);

    void triggerDeliver(Long routeId);

    void scheduleDeliverRetry(Long routeId, int retryCount, int delaySeconds);
}
