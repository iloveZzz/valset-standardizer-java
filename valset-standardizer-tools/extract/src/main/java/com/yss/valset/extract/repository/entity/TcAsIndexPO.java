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
 * 标准资产指标表。
 */
@Data
@TableName("tc_as_index")
public class TcAsIndexPO {

    /** 流水号 */
    @Id
    @TableId(value = "id", type = IdType.INPUT)
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

    /** 实收资本 */
    @TableField("paid_capital")
    private BigDecimal paidCapital;

    /** 资产合计 */
    @TableField("total_assets")
    private BigDecimal totalAssets;

    /** 负债合计 */
    @TableField("total_liabi")
    private BigDecimal totalLiabi;

    /** 资产净值 */
    @TableField("asset_value")
    private BigDecimal assetValue;

    /** 单位净值 */
    @TableField("avg_nav")
    private BigDecimal avgNav;

    /** 累计单位净值 */
    @TableField("acc_net")
    private BigDecimal accNet;

    /** 每万份收益 */
    @TableField("ten_sou_yield")
    private BigDecimal tenSouYield;

    /** 七日年化收益率 */
    @TableField("seven_annu_yield")
    private BigDecimal sevenAnnuYield;

    /** 本日年化收益率 */
    @TableField("today_annu_yield")
    private BigDecimal todayAnnuYield;

    /** 本日收益 */
    @TableField("yield")
    private BigDecimal yield;

    /** 偏离度 */
    @TableField("deviation")
    private BigDecimal deviation;

    /** 偏离金额 */
    @TableField("deviation_amt")
    private BigDecimal deviationAmt;

    /** 资产合计(成本) */
    @TableField("total_assets_cb")
    private BigDecimal totalAssetsCb;

    /** 负债合计(成本) */
    @TableField("total_liabi_cb")
    private BigDecimal totalLiabiCb;

    /** 资产净值(成本) */
    @TableField("asset_value_cb")
    private BigDecimal assetValueCb;

    /** 资产合计(原币成本) */
    @TableField("total_assets_cb_y")
    private BigDecimal totalAssetsCbY;

    /** 负债合计(原币成本) */
    @TableField("total_liabi_cb_y")
    private BigDecimal totalLiabiCbY;

    /** 资产净值(原币成本) */
    @TableField("asset_value_cb_y")
    private BigDecimal assetValueCbY;

    /** 资产合计(原币) */
    @TableField("total_assets_y")
    private BigDecimal totalAssetsY;

    /** 负债合计(原币) */
    @TableField("total_liabi_y")
    private BigDecimal totalLiabiY;

    /** 资产净值(原币) */
    @TableField("asset_value_y")
    private BigDecimal assetValueY;

    /** 实收资本 */
    @TableField("paid_capital_cb")
    private BigDecimal paidCapitalCb;

    /** 指标类型 */
    @TableField("index_type")
    private String indexType;

    /** 时间戳 */
    @TableField("time_stamp")
    private LocalDateTime timeStamp;
}
