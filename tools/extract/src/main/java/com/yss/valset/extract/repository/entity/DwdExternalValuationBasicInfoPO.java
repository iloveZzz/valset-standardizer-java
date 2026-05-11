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
 * DWD 外部估值基础信息表。
 */
@Data
@TableName("t_stg_external_valuation_basic_info")
public class DwdExternalValuationBasicInfoPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("valuation_id")
    private Long valuationId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("info_key")
    private String infoKey;

    @TableField("info_value")
    private String infoValue;
}
