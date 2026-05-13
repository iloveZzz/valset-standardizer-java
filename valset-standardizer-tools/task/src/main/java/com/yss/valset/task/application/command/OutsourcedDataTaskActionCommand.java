package com.yss.valset.task.application.command;

import lombok.Data;

/**
 * 估值表解析任务单条操作命令。
 */
@Data
public class OutsourcedDataTaskActionCommand {

    private String reason;

    private Boolean force;
}
