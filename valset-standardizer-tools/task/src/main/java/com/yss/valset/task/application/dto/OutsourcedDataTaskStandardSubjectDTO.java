package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.Map;

/**
 * 估值标准数据明细行。
 */
@Data
public class OutsourcedDataTaskStandardSubjectDTO implements java.io.Serializable {

    private Long id;

    private Long valuationId;

    private String sheetName;

    private Integer rowDataNumber;

    private String subjectCode;

    private String subjectName;

    private Integer levelNo;

    private String parentCode;

    private String rootCode;

    private Boolean leaf;

    private String rawValuesJson;

    private Map<String, String> rawValues;
}
