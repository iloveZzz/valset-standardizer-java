package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件来源视图。
 */
@Data
@Builder
public class TransferSourceViewDTO {

    /**
     * 文件来源主键。
     */
    @Schema(type = "string", example = "2047183867752460289")
    private String sourceId;
    /**
     * 文件来源编码。
     */
    private String sourceCode;
    /**
     * 文件来源名称。
     */
    private String sourceName;
    /**
     * 文件来源类型。
     */
    private String sourceType;
    /**
     * 当前使用的来源表单模板名。
     */
    private String formTemplateName;
    /**
     * 是否启用。
     */
    private Boolean enabled;
    /**
     * 轮询表达式。
     */
    private String pollCron;
    /**
     * 收取状态。
     */
    private String ingestStatus;
    /**
     * 是否仍在收取锁定中。
     */
    private Boolean ingestBusy;
    /**
     * 收取开始时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime ingestStartedAt;
    /**
     * 收取结束时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime ingestFinishedAt;
    /**
     * 连接配置。
     */
    private Map<String, Object> connectionConfig;
    /**
     * 来源扩展信息。
     */
    private Map<String, Object> sourceMeta;
    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    /**
     * 修改时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
