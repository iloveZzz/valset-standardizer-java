package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 前端发送机构下拉兼容视图。
 */
@Data
@Builder
public class OrgBasicInfoVO implements java.io.Serializable {

    /** 机构ID */
    private String id;

    /** 机构代码 */
    private String orgCd;

    /** 机构全称 */
    private String orgFullNm;

    /** 机构简称 */
    private String orgAbbrNm;

    /** 是否审核 (0:否, 1:是) */
    private Integer isAudt;

    /** 是否删除 (0:否, 1:是) */
    private Integer isDel;
}
