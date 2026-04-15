package com.yss.subjectmatch.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * ODS 原始行数据视图。
 */
@Data
@Builder
public class RawValuationRowDTO {
    private String sheetName;
    private Integer rowDataNumber;
    private List<Object> rowData;
    private Map<String, Object> rowUniverData;
}
