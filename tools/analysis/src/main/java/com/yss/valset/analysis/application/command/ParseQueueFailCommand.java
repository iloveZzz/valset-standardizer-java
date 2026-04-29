package com.yss.valset.analysis.application.command;

import lombok.Data;

/**
 * 待解析任务失败命令。
 */
@Data
public class ParseQueueFailCommand {

    private String errorMessage;
}
