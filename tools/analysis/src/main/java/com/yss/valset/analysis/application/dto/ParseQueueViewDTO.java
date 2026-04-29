package com.yss.valset.analysis.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待解析任务视图。
 */
@Data
@Builder
public class ParseQueueViewDTO {

    private String queueId;

    private String businessKey;

    private String transferId;

    private String originalName;

    private String sourceId;

    private String sourceType;

    private String sourceCode;

    private String routeId;

    private String deliveryId;

    private String tagId;

    private String tagCode;

    private String tagName;

    private String fileStatus;

    private String deliveryStatus;

    private String parseStatus;

    private String triggerMode;

    private Integer retryCount;

    private String subscribedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime subscribedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime parsedAt;

    private String lastErrorMessage;

    private String objectSnapshotJson;

    private String deliverySnapshotJson;

    private String parseRequestJson;

    private String parseResultJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
