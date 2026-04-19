package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 解析规则样例视图。
 */
@Data
@Builder
public class ParseRuleCaseViewDTO {
    private Long id;
    private Long profileId;
    private Long sampleFileId;
    private String sampleFileName;
    private String expectedSheetName;
    private Integer expectedHeaderRow;
    private Integer expectedDataStartRow;
    private Integer expectedSubjectCount;
    private Integer expectedMetricCount;
    private String expectedOutputHash;
    private String status;
}
