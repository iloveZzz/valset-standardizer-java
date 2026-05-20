package com.yss.valset.task.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

/**
 * 估值解析批次原始工作簿下载结果。
 */
@Data
@AllArgsConstructor
public class OutsourcedDataTaskRawWorkbookDownloadDTO implements java.io.Serializable {

    private String batchId;

    private Long fileId;

    private String fileName;

    private String contentType;

    private Long contentLength;

    private Path filePath;
}
