package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 估值数据分析结果聚合。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ParsedValuationData {
    /** 工作簿路径 */
    private String workbookPath;
    /** sheet名称 */
    private String sheetName;
    /** 表头行号 */
    private Integer headerRowNumber;
    /** 数据开始行号 */
    private Integer dataStartRowNumber;
    /** 标题 */
    private String title;
    /** 原始文件名 */
    private String fileNameOriginal;
    /** 基础信息映射 */
    private Map<String, String> basicInfo;
    /** 表头列表 */
    private List<String> headers;
    /** 表头详细信息列表 */
    private List<List<String>> headerDetails;
    /** 表头列元数据 */
    private List<HeaderColumnMeta> headerColumns;
    /** 表头映射决策列表 */
    private List<MappingDecision> headerMappingDecisions;
    /** 映射质量报告 */
    private MappingQualityReport mappingQualityReport;
    /** 主题记录列表 */
    private List<SubjectRecord> subjects;
    /** 指标记录列表 */
    private List<MetricRecord> metrics;
}
