package com.yss.subjectmatch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件接入日志。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMatchFileIngestLog {
    private Long ingestId;
    private Long fileId;
    private SubjectMatchFileSourceChannel sourceChannel;
    private String sourceUri;
    private String channelMessageId;
    private String ingestStatus;
    private LocalDateTime ingestTime;
    private String ingestMetaJson;
    private String createdBy;
    private String errorMessage;
}
