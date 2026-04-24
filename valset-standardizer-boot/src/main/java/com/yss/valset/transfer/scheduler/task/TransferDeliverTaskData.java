package com.yss.valset.transfer.scheduler.task;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件投递任务入参。
 */
public record TransferDeliverTaskData(
        String routeId,
        String transferId,
        int retryCount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
