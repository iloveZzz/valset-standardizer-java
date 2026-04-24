package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件接入日志视图。
 */
@Data
@Builder
public class ValsetFileIngestLogViewDTO {
    /**
     * 接入日志主键。
     */
    private String ingestId;
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 来源渠道。
     */
    private String sourceChannel;
    /**
     * 来源 URI。
     */
    private String sourceUri;
    /**
     * 渠道消息标识。
     */
    private String channelMessageId;
    /**
     * 接入状态。
     */
    private String ingestStatus;
    /**
     * 接入时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime ingestTime;
    /**
     * 接入元数据 JSON。
     */
    private String ingestMetaJson;
    /**
     * 创建人。
     */
    private String createdBy;
    /**
     * 错误信息。
     */
    private String errorMessage;
}
