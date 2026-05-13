package com.yss.valset.file.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件接入日志视图。
 */
@Data
@Builder
public class ValsetFileIngestLogViewDTO implements java.io.Serializable{
    private String ingestId;
    private String fileId;
    private String sourceChannel;
    private String sourceUri;
    private String channelMessageId;
    private String ingestStatus;
    private LocalDateTime ingestTime;
    private String ingestMetaJson;
    private String createdBy;
    private String errorMessage;
}
