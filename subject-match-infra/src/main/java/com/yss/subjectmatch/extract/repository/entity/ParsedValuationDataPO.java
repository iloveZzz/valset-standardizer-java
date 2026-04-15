package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.cloud.sankuai.GenerationTypeSeq;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@TableName("t_subject_match_parsed_workbook")
public class ParsedValuationDataPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("file_id")
    private Long fileId;

    @TableField("workbook_path")
    private String workbookPath;

    @TableField("sheet_name")
    private String sheetName;

    @TableField("header_row_number")
    private Integer headerRowNumber;

    @TableField("data_start_row_number")
    private Integer dataStartRowNumber;

    @TableField("title")
    private String title;

    @TableField("basic_info_json")
    private String basicInfoJson;

    @TableField("headers_json")
    private String headersJson;

    @TableField("header_details_json")
    private String headerDetailsJson;

    @TableField("subjects_json")
    private String subjectsJson;

    @TableField("metrics_json")
    private String metricsJson;
}
