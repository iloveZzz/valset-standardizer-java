package com.yss.valset.transfer.scheduler.task;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件收发运行日志清理任务入参。
 */
public record TransferRunLogCleanupTaskData() implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
