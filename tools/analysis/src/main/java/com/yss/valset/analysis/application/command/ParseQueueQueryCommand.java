package com.yss.valset.analysis.application.command;

import lombok.Data;

/**
 * 待解析任务查询条件。
 */
@Data
public class ParseQueueQueryCommand {

    private String transferId;

    private String businessKey;

    private String sourceCode;

    private String routeId;

    private String tagCode;

    private String fileStatus;

    private String deliveryStatus;

    private String parseStatus;

    private String triggerMode;

    private Integer pageIndex;

    private Integer pageSize;
}
