package com.yss.valset.application.command;

import lombok.Data;

/**
 * 解析规则样例新增或更新请求。
 */
@Data
public class ParseRuleCaseUpsertCommand {
    private Long id;
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
