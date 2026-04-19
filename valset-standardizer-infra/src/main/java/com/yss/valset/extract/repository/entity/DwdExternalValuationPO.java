package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.cloud.sankuai.GenerationTypeSeq;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * DWD 外部估值主表。
 */
@Data
@TableName("t_stg_external_valuation")
public class DwdExternalValuationPO {

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
}
