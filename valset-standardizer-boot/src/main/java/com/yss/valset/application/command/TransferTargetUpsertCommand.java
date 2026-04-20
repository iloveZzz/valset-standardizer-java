package com.yss.valset.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 投递目标新增或更新命令。
 */
@Data
public class TransferTargetUpsertCommand {

    private Long targetId;

    @NotBlank(message = "目标编码不能为空")
    private String targetCode;

    @NotBlank(message = "目标名称不能为空")
    private String targetName;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    private Boolean enabled = Boolean.TRUE;

    private String targetPathTemplate;

    private Map<String, Object> connectionConfig;

    private Map<String, Object> targetMeta;
}
