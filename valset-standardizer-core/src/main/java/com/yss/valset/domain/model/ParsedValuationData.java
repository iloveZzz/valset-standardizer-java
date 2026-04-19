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
    private String workbookPath;
    private String sheetName;
    private Integer headerRowNumber;
    private Integer dataStartRowNumber;
    private String title;
    private String fileNameOriginal;
    private Map<String, String> basicInfo;
    private List<String> headers;
    private List<List<String>> headerDetails;
    private List<HeaderColumnMeta> headerColumns;
    private List<MappingDecision> headerMappingDecisions;
    private MappingQualityReport mappingQualityReport;
    private List<SubjectRecord> subjects;
    private List<MetricRecord> metrics;
}
