package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件接入日志视图。
 */
@Data
@Builder
public class ValsetFileIngestLogViewDTO {
    private Long ingestId;
    private Long fileId;
    private String sourceChannel;
    private String sourceUri;
    private String channelMessageId;
    private String ingestStatus;
    private LocalDateTime ingestTime;
    private String ingestMetaJson;
    private String createdBy;
    private String errorMessage;
}
