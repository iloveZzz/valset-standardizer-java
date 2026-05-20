package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 已解析的工作簿主题行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRecord {
    /** sheet名称 */
    private String sheetName;
    /** 数据行号 */
    private Integer rowDataNumber;
    /** 主题代码 */
    private String subjectCode;
    /** 主题名称 */
    private String subjectName;
    /** 层级 */
    private Integer level;
    /** 父级代码 */
    private String parentCode;
    /** 根代码 */
    private String rootCode;
    /** 段数 */
    private Integer segmentCount;
    /** 路径代码列表 */
    private List<String> pathCodes;
    /** 是否为叶子节点 */
    private Boolean leaf;
    /** 原始值列表 */
    private List<Object> rawValues;
    /** 标准代码 */
    private String standardCode;
    /** 标准名称 */
    private String standardName;
    /** 标准值映射 */
    private Map<String, Object> standardValues;
    /** 映射规则ID */
    private Long mappingRuleId;
    /** 映射数据源ID */
    private Long mappingSourceId;
    /** 映射状态 */
    private String mappingStatus;
    /** 映射原因 */
    private String mappingReason;
    /** 匹配置信度 */
    private Double mappingConfidence;
}
