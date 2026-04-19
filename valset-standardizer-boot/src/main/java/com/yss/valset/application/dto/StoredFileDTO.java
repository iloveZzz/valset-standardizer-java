package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 上传文件落盘后的描述信息。
 */
@Data
@Builder
public class StoredFileDTO {
    private String originalFilename;
    private String storedFilename;
    private String absolutePath;
    private String dataSourceType;
    private Long fileSizeBytes;
    private String fileFingerprint;
}
