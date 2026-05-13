package com.yss.valset.transfer.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 投递目标新增或更新命令。
 */
@Data
public class TransferTargetUpsertCommand {

    /**
     * 投递目标主键。
     */
    private String targetId;

    /**
     * 投递目标编码。
     */
    @NotBlank(message = "目标编码不能为空")
    private String targetCode;

    /**
     * 投递目标名称。
     */
    @NotBlank(message = "目标名称不能为空")
    private String targetName;

    /**
     * 投递目标类型。
     */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /**
     * 是否启用。
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * 投递路径模板。
     */
    private String targetPathTemplate;

    /**
     * 连接配置。
     */
    private Map<String, Object> connectionConfig;

    /**
     * 目标扩展信息。
     */
    private Map<String, Object> targetMeta;
}
