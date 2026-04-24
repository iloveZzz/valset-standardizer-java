package com.yss.valset.transfer.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件主对象查询视图。
 */
@Data
@Builder
public class TransferObjectViewDTO {

    /**
     * 文件主键。
     */
    private String transferId;
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
     * 原始文件名。
     */
    private String originalName;
    /**
     * 文件扩展名。
     */
    private String extension;
    /**
     * 文件类型。
     */
    private String mimeType;
    /**
     * 文件大小，单位字节。
     */
    private String sizeBytes;
    /**
     * 文件指纹。
     */
    private String fingerprint;
    /**
     * 来源引用标识。
     */
    private String sourceRef;
    /**
     * 邮件唯一标识。
     */
    private String mailId;
    /**
     * 邮件发件人。
     */
    private String mailFrom;
    /**
     * 邮件收件人。
     */
    private String mailTo;
    /**
     * 邮件抄送人。
     */
    private String mailCc;
    /**
     * 邮件密送人。
     */
    private String mailBcc;
    /**
     * 邮件主题。
     */
    private String mailSubject;
    /**
     * 邮件正文。
     */
    private String mailBody;
    /**
     * 邮件协议。
     */
    private String mailProtocol;
    /**
     * 邮件文件夹。
     */
    private String mailFolder;
    /**
     * 本地临时文件路径。
     */
    private String localTempPath;
    /**
     * 文件状态。
     */
    private String status;
    /**
     * 收取时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedAt;
    /**
     * 落库时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime storedAt;
    /**
     * 路由主键。
     */
    private String routeId;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 文件元数据 JSON。
     */
    private String fileMetaJson;
}
