package com.yss.valset.analysis.application.command;

import lombok.Data;

/**
 * 待解析任务完成命令。
 */
@Data
public class ParseQueueCompleteCommand {

    private Object parseResultJson;
}
