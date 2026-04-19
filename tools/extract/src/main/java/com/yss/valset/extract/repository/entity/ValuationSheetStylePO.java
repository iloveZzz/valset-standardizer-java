package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 估值表 sheet 级样式快照 ODS 表。
 */
@Data
@TableName("t_ods_valuation_sheet_style")
public class ValuationSheetStylePO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("file_id")
    private Long fileId;

    @TableField("sheet_name")
    private String sheetName;

    @TableField("style_scope")
    private String styleScope;

    @TableField("sheet_style_json")
    private String sheetStyleJson;

    @TableField("preview_row_count")
    private Integer previewRowCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
