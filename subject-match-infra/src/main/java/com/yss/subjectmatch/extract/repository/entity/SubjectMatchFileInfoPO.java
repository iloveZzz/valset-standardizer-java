package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@TableName("t_subject_match_file_info")
public class SubjectMatchFileInfoPO {
    @Id
    @TableId(value = "file_id", type = IdType.ASSIGN_ID)
    private Long fileId;

    @TableField("file_name_original")
    private String fileNameOriginal;

    @TableField("file_name_normalized")
    private String fileNameNormalized;

    @TableField("file_extension")
    private String fileExtension;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_size_bytes")
    private Long fileSizeBytes;

    @TableField("file_fingerprint")
    private String fileFingerprint;

    @TableField("source_channel")
    private String sourceChannel;

    @TableField("source_uri")
    private String sourceUri;

    @TableField("storage_type")
    private String storageType;

    @TableField("storage_uri")
    private String storageUri;

    @TableField("file_format")
    private String fileFormat;

    @TableField("file_status")
    private String fileStatus;

    @TableField("created_by")
    private String createdBy;

    @TableField("received_at")
    private LocalDateTime receivedAt;

    @TableField("stored_at")
    private LocalDateTime storedAt;

    @TableField("last_processed_at")
    private LocalDateTime lastProcessedAt;

    @TableField("last_task_id")
    private Long lastTaskId;

    @TableField("error_message")
    private String errorMessage;

    @TableField("source_meta_json")
    private String sourceMetaJson;

    @TableField("storage_meta_json")
    private String storageMetaJson;

    @TableField("remark")
    private String remark;
}
