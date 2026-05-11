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
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 工作簿路径。
     */
    private String workbookPath;
    /**
     * 工作表名称。
     */
    private String sheetName;
    /**
     * 表头行号。
     */
    private Integer headerRowNumber;
    /**
     * 数据起始行号。
     */
    private Integer dataStartRowNumber;
    /**
     * 原始文件名。
     */
    private String fileNameOriginal;
    /**
     * 标题。
     */
    private String title;
    /**
     * 基础信息。
     */
    private Map<String, String> basicInfo;
    /**
     * 表头内容。
     */
    private List<String> headers;
    /**
     * 表头明细。
     */
    private List<List<String>> headerDetails;
    /**
     * 表头元数据。
     */
    private List<HeaderColumnMeta> headerColumns;
    /**
     * 科目明细。
     */
    private List<SubjectRecord> subjects;
    /**
     * 指标明细。
     */
    private List<MetricRecord> metrics;
}
