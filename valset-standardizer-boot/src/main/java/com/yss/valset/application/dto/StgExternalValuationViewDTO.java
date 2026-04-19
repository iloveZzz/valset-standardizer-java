package com.yss.valset.application.dto;

import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.SubjectRecord;
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
    private String fileNameOriginal;
    private String title;
    private Map<String, String> basicInfo;
    private List<String> headers;
    private List<List<String>> headerDetails;
    private List<HeaderColumnMeta> headerColumns;
    private List<SubjectRecord> subjects;
    private List<MetricRecord> metrics;
}
