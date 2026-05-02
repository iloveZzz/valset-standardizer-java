package com.yss.valset.task.application.command;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 估值表解析任务批量操作命令。
 */
@Data
public class OutsourcedDataTaskBatchCommand {

    @NotEmpty(message = "任务批次不能为空")
    private List<String> batchIds;

    private String reason;
}
