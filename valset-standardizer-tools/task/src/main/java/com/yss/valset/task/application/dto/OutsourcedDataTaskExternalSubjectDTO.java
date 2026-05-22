package com.yss.valset.task.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 委外估值明细行。
 */
@Data
public class OutsourcedDataTaskExternalSubjectDTO implements java.io.Serializable {

    private Long id;

    private String orgCd;

    private String pdCd;

    private String bizDate;

    private String subjectCd;

    private String subjectNm;

    private String paSubjectCd;

    private String paSubjectNm;

    @JsonProperty("nHldamt")
    private BigDecimal nHldamt;

    @JsonProperty("nHldcst")
    private BigDecimal nHldcst;

    @JsonProperty("nHldcstLocl")
    private BigDecimal nHldcstLocl;

    @JsonProperty("nHldmkv")
    private BigDecimal nHldmkv;

    @JsonProperty("nHldmkvLocl")
    private BigDecimal nHldmkvLocl;

    @JsonProperty("nHldvva")
    private BigDecimal nHldvva;

    @JsonProperty("nHldvvaL")
    private BigDecimal nHldvvaL;

    private String ccyCd;

    @JsonProperty("nValrate")
    private BigDecimal nValrate;

    @JsonProperty("nPriceCost")
    private BigDecimal nPriceCost;

    @JsonProperty("nValprice")
    private BigDecimal nValprice;

    @JsonProperty("nCbJzBl")
    private BigDecimal nCbJzBl;

    @JsonProperty("nSzJzBl")
    private BigDecimal nSzJzBl;

    @JsonProperty("nZcBl")
    private BigDecimal nZcBl;

    private String suspInfo;

    private String valuatEquity;

    private String finAttrIdD;

    private String finMktCd;

    private String timeStamp;

    private String consFloatTpCd;

    private String sourceTp;

    private String sourceSign;

    private Integer sn;

    private String dataDt;

    private String isinCd;

    private Map<String, String> rawValues;
}
