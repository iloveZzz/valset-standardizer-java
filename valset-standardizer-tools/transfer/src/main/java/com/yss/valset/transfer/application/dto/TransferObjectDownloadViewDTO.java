package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * 文件主对象下载视图。
 */
@Data
@Builder
public class TransferObjectDownloadViewDTO {

    /**
     * 文件主键。
     */
    private String transferId;
    /**
     * 本地临时文件路径。
     */
    private Path filePath;
    /**
     * 下载文件名。
     */
    private String fileName;
    /**
     * 文件类型。
     */
    private String contentType;
    /**
     * 文件大小。
     */
    private Long contentLength;
}
