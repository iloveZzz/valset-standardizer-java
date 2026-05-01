package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@TableName("t_valset_file_ingest_log")
public class ValsetFileIngestLogPO {
    @Id
    @TableId(value = "ingest_id", type = IdType.ASSIGN_ID)
    private Long ingestId;

    @TableField("file_id")
    private Long fileId;

    @TableField("source_channel")
    private String sourceChannel;

    @TableField("source_uri")
    private String sourceUri;

    @TableField("channel_message_id")
    private String channelMessageId;

    @TableField("ingest_status")
    private String ingestStatus;

    @TableField("ingest_time")
    private LocalDateTime ingestTime;

    @TableField("ingest_meta_json")
    private String ingestMetaJson;

    @TableField("created_by")
    private String createdBy;

    @TableField("error_message")
    private String errorMessage;
}
