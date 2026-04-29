package com.yss.valset.analysis.application.command;

import lombok.Data;

/**
 * 待解析任务补漏命令。
 */
@Data
public class ParseQueueBackfillCommand {

    private String transferId;

    private String sourceId;

    private String sourceCode;

    private String routeId;

    private String tagCode;

    private String status;

    private String deliveryStatus;

    private String parseStatus;

    private Boolean forceRebuild = Boolean.FALSE;

    private Boolean dryRun = Boolean.FALSE;
}
