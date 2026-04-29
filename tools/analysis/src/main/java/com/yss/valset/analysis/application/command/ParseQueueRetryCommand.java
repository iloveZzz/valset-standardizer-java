package com.yss.valset.analysis.application.command;

import lombok.Data;

/**
 * 待解析任务重试命令。
 */
@Data
public class ParseQueueRetryCommand {

    private Boolean forceRebuild = Boolean.FALSE;
}
