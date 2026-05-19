package com.yss.valset.transfer.domain.model;

import lombok.Data;

/**
 * 产品识别规则。
 */
@Data
public class ProductMatchRule {

    private String id;
    private String fileTypeName;
    private String pdCd;
    private String pdNm;
    private String orgCd;
    private String orgNm;
    private String pdType;
    private String fileType;
    private String matchRules;
    private String jobName;
    private String jobScene;

    public ProductMatchRule() {
    }

    public ProductMatchRule(String id,
                            String fileTypeName,
                            String pdCd,
                            String pdNm,
                            String orgCd,
                            String orgNm,
                            String pdType,
                            String fileType,
                            String matchRules,
                            String jobName,
                            String jobScene) {
        this.id = id;
        this.fileTypeName = fileTypeName;
        this.pdCd = pdCd;
        this.pdNm = pdNm;
        this.orgCd = orgCd;
        this.orgNm = orgNm;
        this.pdType = pdType;
        this.fileType = fileType;
        this.matchRules = matchRules;
        this.jobName = jobName;
        this.jobScene = jobScene;
    }

}
