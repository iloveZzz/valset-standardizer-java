package com.yss.valset.domain.model;

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
public class ValsetFileIngestLog {
    /** 接入ID */
    private Long ingestId;
    /** 文件ID */
    private Long fileId;
    /** 来源渠道 */
    private ValsetFileSourceChannel sourceChannel;
    /** 来源URI */
    private String sourceUri;
    /** 渠道消息ID */
    private String channelMessageId;
    /** 接入状态 */
    private String ingestStatus;
    /** 接入时间 */
    private LocalDateTime ingestTime;
    /** 接入元数据JSON */
    private String ingestMetaJson;
    /** 创建人 */
    private String createdBy;
    /** 错误消息 */
    private String errorMessage;
}
