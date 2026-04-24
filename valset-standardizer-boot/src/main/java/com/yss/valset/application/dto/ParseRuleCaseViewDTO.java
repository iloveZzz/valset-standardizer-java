package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 解析规则样例视图。
 */
@Data
@Builder
public class ParseRuleCaseViewDTO {
    /**
     * 样例主键。
     */
    private String id;
    /**
     * 模板主键。
     */
    private String profileId;
    /**
     * 样例文件主键。
     */
    private String sampleFileId;
    /**
     * 样例文件名称。
     */
    private String sampleFileName;
    /**
     * 期望的工作表名称。
     */
    private String expectedSheetName;
    /**
     * 期望的表头行号。
     */
    private Integer expectedHeaderRow;
    /**
     * 期望的数据起始行号。
     */
    private Integer expectedDataStartRow;
    /**
     * 期望的科目数量。
     */
    private Integer expectedSubjectCount;
    /**
     * 期望的指标数量。
     */
    private Integer expectedMetricCount;
    /**
     * 期望输出哈希。
     */
    private String expectedOutputHash;
    /**
     * 样例状态。
     */
    private String status;
}
