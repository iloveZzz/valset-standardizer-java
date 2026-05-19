package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 估值标准数据基础信息视图。
 */
@Data
public class OutsourcedDataTaskStandardBasicDTO implements java.io.Serializable {

    private String batchId;

    private Long valuationId;

    private Long fileId;

    private Long taskId;

    private String workbookPath;

    private String sheetName;

    private String title;

    private Integer headerRowNumber;

    private Integer dataStartRowNumber;

    private Long basicInfoCount;

    private Long subjectCount;

    private Long metricCount;

    private List<OutsourcedDataTaskStandardBasicRowDTO> basicRows;

    private List<OutsourcedDataTaskStandardRawColumnDTO> rawColumns;
}
