package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 已解析工作簿的摘要统计信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbookSummary {
    private String fileNameOriginal;
    private String title;
    private String sheetName;
    private Integer headerRowNumber;
    private Integer dataStartRowNumber;
    private Map<String, String> basicInfo;
    private Integer subjectCount;
    private Integer leafSubjectCount;
    private Integer nonLeafSubjectCount;
    private Integer metricCount;
    private Integer metricRowCount;
    private Integer metricDataCount;
    private Integer rootSubjectCount;
    private Integer maxLevel;
    private List<String> duplicateSubjectCodes;
    private Map<Integer, Integer> levelDistribution;
}
