package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析规则回归样例对象。
 */
@Data
@TableName("t_file_parse_case")
public class ParseRuleCasePO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("profile_id")
    private Long profileId;

    @TableField("sample_file_id")
    private Long sampleFileId;

    @TableField("sample_file_name")
    private String sampleFileName;

    @TableField("expected_sheet_name")
    private String expectedSheetName;

    @TableField("expected_header_row")
    private Integer expectedHeaderRow;

    @TableField("expected_data_start_row")
    private Integer expectedDataStartRow;

    @TableField("expected_subject_count")
    private Integer expectedSubjectCount;

    @TableField("expected_metric_count")
    private Integer expectedMetricCount;

    @TableField("expected_output_hash")
    private String expectedOutputHash;

    @TableField("status")
    private String status;

    @TableField("creater")
    private String creater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("modifier")
    private String modifier;

    @TableField("modify_time")
    private LocalDateTime modifyTime;
}
