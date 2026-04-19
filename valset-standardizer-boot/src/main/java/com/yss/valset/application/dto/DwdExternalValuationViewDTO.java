package com.yss.valset.application.dto;

import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.SubjectRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DWD 外部估值标准数据视图。
 */
@Data
@Builder
public class DwdExternalValuationViewDTO {
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
