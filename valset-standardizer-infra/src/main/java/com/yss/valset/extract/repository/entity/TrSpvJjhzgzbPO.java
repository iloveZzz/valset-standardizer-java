package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资管持仓估值表（tr_spv_jjhzgzb）。
 */
@Data
@TableName("tr_spv_jjhzgzb")
public class TrSpvJjhzgzbPO {
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

    /** 科目代码 */
    @TableField("subject_cd")
    private String subjectCd;

    /** 科目名称 */
    @TableField("subject_nm")
    private String subjectNm;

    /** 父科目代码 */
    @TableField("pa_subject_cd")
    private String paSubjectCd;

    /** 父级科目名称 */
    @TableField("pa_subject_nm")
    private String paSubjectNm;

    /** 持仓数量 */
    @TableField("n_hldamt")
    private BigDecimal nHldamt;

    /** 原币持仓成本 */
    @TableField("n_hldcst")
    private BigDecimal nHldcst;

    /** 本币持仓成本 */
    @TableField("n_hldcst_locl")
    private BigDecimal nHldcstLocl;

    /** 原币持仓市值 */
    @TableField("n_hldmkv")
    private BigDecimal nHldmkv;

    /** 本币持仓市值 */
    @TableField("n_hldmkv_locl")
    private BigDecimal nHldmkvLocl;

    /** 原币证券估增 */
    @TableField("n_hldvva")
    private BigDecimal nHldvva;

    /** 本币证券估值 */
    @TableField("n_hldvva_l")
    private BigDecimal nHldvvaL;

    /** 币种代码 */
    @TableField("ccy_cd")
    private String ccyCd;

    /** 货币估值汇率 */
    @TableField("n_valrate")
    private BigDecimal nValrate;

    /** 单位成本 */
    @TableField("n_price_cost")
    private BigDecimal nPriceCost;

    /** 证券估值行情 */
    @TableField("n_valprice")
    private BigDecimal nValprice;

    /** 本币成本占比 */
    @TableField("n_cb_jz_bl")
    private BigDecimal nCbJzBl;

    /** 本币市值占比 */
    @TableField("n_sz_jz_bl")
    private BigDecimal nSzJzBl;

    /** 资产占比 */
    @TableField("n_zc_bl")
    private BigDecimal nZcBl;

    /** 停牌信息 */
    @TableField("susp_info")
    private String suspInfo;

    /** 估值权益 */
    @TableField("valuat_equity")
    private String valuatEquity;

    /** 财务属性-投资意图 */
    @TableField("fin_attr_id_d")
    private String finAttrIdD;

    /** 财务市场代码 */
    @TableField("fin_mkt_cd")
    private String finMktCd;

    /** 时间戳 */
    @TableField("time_stamp")
    private LocalDateTime timeStamp;

    /** 受限流通类别代码 */
    @TableField("cons_float_tp_cd")
    private String consFloatTpCd;

    /** 来源类型 */
    @TableField("source_tp")
    private String sourceTp;

    /** 来源标记 */
    @TableField("source_sign")
    private String sourceSign;

    /** 序号 */
    @TableField("sn")
    private Integer sn;

    /** 数据日期 */
    @TableField("data_dt")
    private String dataDt;

    /** ISIN代码 */
    @TableField("isin_cd")
    private String isinCd;
}
