package com.yss.valset.transfer.scheduler.task;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分拣对象定时重投递任务入参。
 */
public record TransferObjectRedeliverTaskData() implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
