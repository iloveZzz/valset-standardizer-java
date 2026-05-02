package com.yss.valset.task.application.command;

import lombok.Data;

/**
 * 估值表解析任务分页查询条件。
 */
@Data
public class OutsourcedDataTaskQueryCommand {

    private String businessDate;

    private String managerName;

    private String productKeyword;

    private String stage;

    private String status;

    private String sourceType;

    private String errorType;

    private Boolean includeHistory;

    private Integer pageIndex;

    private Integer pageSize;
}
