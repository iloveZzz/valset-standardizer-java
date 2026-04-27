package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 邮件附件视图。
 */
@Data
@Builder
public class TransferObjectAttachmentViewDTO {

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 原始文件名。
     */
    private String originalName;

    /**
     * 本地临时文件路径。
     */
    private String localTempPath;

    /**
     * 真实文件存储地址。
     */
    private String realStoragePath;

    /**
     * 文件类型。
     */
    private String mimeType;

    /**
     * 文件大小，单位字节。
     */
    private String sizeBytes;
}
