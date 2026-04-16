package com.yss.subjectmatch.application.dto;

import com.yss.subjectmatch.domain.model.HeaderColumnMeta;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * STG 外部估值解析快照视图。
 */
@Data
@Builder
public class StgExternalValuationViewDTO {
    private Long fileId;
    private String workbookPath;
    private String sheetName;
    private Integer headerRowNumber;
    private Integer dataStartRowNumber;
    private String title;
    private Map<String, String> basicInfo;
    private List<String> headers;
    private List<List<String>> headerDetails;
    private List<HeaderColumnMeta> headerColumns;
    private List<SubjectRecord> subjects;
    private List<MetricRecord> metrics;
}
