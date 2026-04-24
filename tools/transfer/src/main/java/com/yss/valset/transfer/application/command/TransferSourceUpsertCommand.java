package com.yss.valset.transfer.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 文件来源新增或更新命令。
 */
@Data
public class TransferSourceUpsertCommand {

    /**
     * 文件来源主键。
     */
    private String sourceId;

    /**
     * 文件来源编码。
     */
    @NotBlank(message = "来源编码不能为空")
    private String sourceCode;

    /**
     * 文件来源名称。
     */
    @NotBlank(message = "来源名称不能为空")
    private String sourceName;

    /**
     * 文件来源类型。
     */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /**
     * 是否启用。
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * 轮询表达式。
     */
    private String pollCron;

    /**
     * 连接配置。
     */
    private Map<String, Object> connectionConfig;

    /**
     * 来源扩展信息。
     */
    private Map<String, Object> sourceMeta;
}
