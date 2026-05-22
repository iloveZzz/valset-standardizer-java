package com.yss.valset.task.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 委外指标明细行。
 */
@Data
public class OutsourcedDataTaskExternalMetricDTO implements java.io.Serializable {

    private Long id;

    private String orgCd;

    private String pdCd;

    private String bizDate;

    private BigDecimal paidCapital;

    private BigDecimal totalAssets;

    private BigDecimal totalLiabi;

    private BigDecimal assetValue;

    private BigDecimal avgNav;

    private BigDecimal accNet;

    private BigDecimal tenSouYield;

    private BigDecimal sevenAnnuYield;

    private BigDecimal todayAnnuYield;

    private BigDecimal yield;

    private BigDecimal deviation;

    private BigDecimal deviationAmt;

    private BigDecimal totalAssetsCb;

    private BigDecimal totalLiabiCb;

    private BigDecimal assetValueCb;

    private BigDecimal totalAssetsCbY;

    private BigDecimal totalLiabiCbY;

    private BigDecimal assetValueCbY;

    private BigDecimal totalAssetsY;

    private BigDecimal totalLiabiY;

    private BigDecimal assetValueY;

    private BigDecimal paidCapitalCb;

    private String indexType;

    private String timeStamp;

    private Map<String, String> rawValues;
}
