package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 上传文件落盘后的描述信息。
 */
@Data
@Builder
public class StoredFileDTO {
    /**
     * 原始文件名。
     */
    private String originalFilename;
    /**
     * 落盘后的文件名。
     */
    private String storedFilename;
    /**
     * 绝对路径。
     */
    private String absolutePath;
    /**
     * 数据源类型。
     */
    private String dataSourceType;
    /**
     * 文件大小，单位字节。
     */
    private Long fileSizeBytes;
    /**
     * 文件指纹。
     */
    private String fileFingerprint;
    /**
     * 文件服务任务标识。
     */
    private String filesysTaskId;
    /**
     * 文件服务文件标识。
     */
    private String filesysFileId;
    /**
     * 文件服务对象键。
     */
    private String filesysObjectKey;
    /**
     * 文件服务原始名称。
     */
    private String filesysOriginalName;
    /**
     * 文件服务 MIME 类型。
     */
    private String filesysMimeType;
    /**
     * 是否已立即上传到文件服务。
     */
    private Boolean filesysInstantUpload;
    /**
     * 文件服务父目录标识。
     */
    private String filesysParentId;
    /**
     * 文件服务存储配置标识。
     */
    private String filesysStorageSettingId;
}
