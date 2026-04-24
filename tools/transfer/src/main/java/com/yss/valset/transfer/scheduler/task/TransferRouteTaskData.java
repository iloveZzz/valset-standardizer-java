package com.yss.valset.transfer.scheduler.task;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件路由任务入参。
 */
public record TransferRouteTaskData(
        String transferId
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
