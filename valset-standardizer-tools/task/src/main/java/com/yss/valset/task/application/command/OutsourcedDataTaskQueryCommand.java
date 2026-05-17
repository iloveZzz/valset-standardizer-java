package com.yss.valset.task.application.command;

import lombok.Data;

/**
 * 估值表解析任务分页查询条件。
 */
@Data
public class OutsourcedDataTaskQueryCommand {

    private String batchId;

    private String taskDate;

    private String businessDate;

    private String managerName;

    private String productKeyword;

    private String taskStage;

    private String stage;

    private String step;

    private String status;

    private String sourceType;

    private String errorType;

    private Integer pageIndex;

    private Integer pageSize;
}
