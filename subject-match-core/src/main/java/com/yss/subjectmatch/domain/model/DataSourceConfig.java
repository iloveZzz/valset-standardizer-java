package com.yss.subjectmatch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 估值表数据源配置信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceConfig {
    
    /**
     * 数据源类型（Excel、CSV、API、DB）。
     */
    private DataSourceType sourceType;
    
    /**
     * 数据源资源定位符或路径参数（例如文件路径、API URL）。
     */
    private String sourceUri;
    
    /**
     * 附加参数（例如原始数据文件 ID、表名、认证 token）。
     */
    private String additionalParams;
}
