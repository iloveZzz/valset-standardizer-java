package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从评估工作簿加载的原始映射示例。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingSample {
    /** 机构名称 */
    private String orgName;
    /** 机构ID */
    private String orgId;
    /** 外部代码 */
    private String externalCode;
    /** 外部名称 */
    private String externalName;
    /** 标准代码 */
    private String standardCode;
    /** 标准名称 */
    private String standardName;
    /** 标准体系 */
    private String standardSystem;
    /** 系统名称 */
    private String systemName;
}
