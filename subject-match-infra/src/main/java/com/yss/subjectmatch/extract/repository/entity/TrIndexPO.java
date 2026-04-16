package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 资产估值指标信息（原始数据）。
 */
@Data
@TableName("t_tr_index")
public class TrIndexPO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 机构代码 */
    @TableField("org_cd")
    private String orgCd;

    /** 产品代码 */
    @TableField("pd_cd")
    private String pdCd;

    /** 业务日期 */
    @TableField("biz_date")
    private String bizDate;

    /** 指标名称 */
    @TableField("indx_nm")
    private String indxNm;

    /** 指标值 */
    @TableField("indx_valu")
    private String indxValu;

    /** 来源类型 */
    @TableField("source_tp")
    private String sourceTp;

    /** 来源标记 */
    @TableField("source_sign")
    private String sourceSign;

    /** 时间戳 */
    @TableField("time_stamp")
    private LocalDateTime timeStamp;

}
