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
 * DWD 外部估值表头表。
 */
@Data
@TableName("t_stg_external_valuation_header")
public class DwdExternalValuationHeaderPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("valuation_id")
    private Long valuationId;

    @TableField("column_index")
    private Integer columnIndex;

    @TableField("header_name")
    private String headerName;

    @TableField("header_detail_json")
    private String headerDetailJson;

    @TableField("header_column_meta_json")
    private String headerColumnMetaJson;
}
