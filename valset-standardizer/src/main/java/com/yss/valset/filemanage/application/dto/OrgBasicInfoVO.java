package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 前端发送机构下拉兼容视图。
 */
@Data
@Builder
public class OrgBasicInfoVO implements java.io.Serializable {

    private String id;

    private String orgCd;

    private String orgFullNm;

    private String orgAbbrNm;

    private Integer isAudt;

    private Integer isDel;
}
