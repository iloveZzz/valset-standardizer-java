package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析规则发布日志对象。
 */
@Data
@TableName("t_file_parse_publish_log")
public class ParseRulePublishLogPO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("profile_id")
    private Long profileId;

    @TableField("version")
    private String version;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("publisher")
    private String publisher;

    @TableField("publish_comment")
    private String publishComment;

    @TableField("validation_result_json")
    private String validationResultJson;

    @TableField("rollback_from_version")
    private String rollbackFromVersion;
}
