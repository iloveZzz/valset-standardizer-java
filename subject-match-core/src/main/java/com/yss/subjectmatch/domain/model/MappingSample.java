package com.yss.subjectmatch.domain.model;

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
    private String orgName;
    private String orgId;
    private String externalCode;
    private String externalName;
    private String standardCode;
    private String standardName;
    private String standardSystem;
    private String systemName;
}
