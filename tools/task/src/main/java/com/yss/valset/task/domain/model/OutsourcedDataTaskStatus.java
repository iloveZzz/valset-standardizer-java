package com.yss.valset.task.domain.model;

import lombok.Getter;

/**
 * 委外数据任务状态。
 */
@Getter
public enum OutsourcedDataTaskStatus {

    PENDING("待处理"),
    RUNNING("处理中"),
    SUCCESS("已完成"),
    FAILED("失败"),
    STOPPED("已停止"),
    BLOCKED("阻塞");

    private final String label;

    OutsourcedDataTaskStatus(String label) {
        this.label = label;
    }
}
