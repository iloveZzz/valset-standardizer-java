package com.yss.subjectmatch.domain.model;

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
    private String sheetName;
    private Integer rowDataNumber;
    private String subjectCode;
    private String subjectName;
    private Integer level;
    private String parentCode;
    private String rootCode;
    private Integer segmentCount;
    private List<String> pathCodes;
    private Boolean leaf;
    private List<Object> rawValues;
    private String standardCode;
    private String standardName;
    private Map<String, Object> standardValues;
    private Long mappingRuleId;
    private Long mappingSourceId;
    private String mappingStatus;
    private String mappingReason;
    private Double mappingConfidence;
}
