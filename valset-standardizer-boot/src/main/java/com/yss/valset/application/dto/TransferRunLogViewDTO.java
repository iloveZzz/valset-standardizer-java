package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件收发运行日志视图。
 */
@Data
@Builder
public class TransferRunLogViewDTO {

    /**
     * 运行日志主键。
     */
    private String runLogId;

    /**
     * 来源主键。
     */
    private String sourceId;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源编码。
     */
    private String sourceCode;

    /**
     * 来源名称。
     */
    private String sourceName;

    /**
     * 路由名称。
     */
    private String routeName;

    /**
     * 目标名称。
     */
    private String targetName;

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 原始文件名称。
     */
    private String originalName;

    /**
     * 路由主键。
     */
    private String routeId;

    /**
     * 触发类型。
     */
    private String triggerType;

    /**
     * 运行阶段。
     */
    private String runStage;

    /**
     * 运行状态。
     */
    private String runStatus;

    /**
     * 运行说明。
     */
    private String logMessage;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
