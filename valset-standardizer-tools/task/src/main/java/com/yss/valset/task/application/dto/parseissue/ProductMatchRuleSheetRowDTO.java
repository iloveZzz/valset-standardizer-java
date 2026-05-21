package com.yss.valset.task.application.dto.parseissue;

import lombok.Data;

import java.io.Serializable;

/**
 * 产品识别规则 Sheet 行。
 */
@Data
public class ProductMatchRuleSheetRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String fileTypeName;

    private String pdCd;

    private String pdNm;

    private String orgCd;

    private String orgNm;

    private String pdType;

    private String subjectSystem;

    private String holdingStatus;

    private String establishedDate;

    private String effectiveFrequency;

    private String delayDays;

    private String approvalRequired;

    private String fileType;

    private String matchRules;

    private String isValid;

    private String memo;

    private String debugName;

    private String jobName;

    private String jobScene;

    private String creater;

    private String createTime;

    private String modifier;

    private String modifyTime;
}
