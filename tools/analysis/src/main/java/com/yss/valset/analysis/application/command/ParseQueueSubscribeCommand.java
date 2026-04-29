package com.yss.valset.analysis.application.command;

import lombok.Data;

/**
 * 待解析任务订阅命令。
 */
@Data
public class ParseQueueSubscribeCommand {

    /**
     * 订阅人。
     */
    private String subscribedBy;
}
