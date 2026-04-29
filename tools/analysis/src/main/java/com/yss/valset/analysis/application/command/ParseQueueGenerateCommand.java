package com.yss.valset.analysis.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 待解析任务生成命令。
 */
@Data
public class ParseQueueGenerateCommand {

    /**
     * 文件主键。
     */
    @NotBlank(message = "文件主键不能为空")
    private String transferId;

    /**
     * 业务键。
     */
    private String businessKey;

    /**
     * 来源主键。
     */
    private String sourceId;

    /**
     * 路由主键。
     */
    private String routeId;

    /**
     * 标签编码。
     */
    private String tagCode;

    /**
     * 是否强制重建。
     */
    private Boolean forceRebuild = Boolean.FALSE;
}
