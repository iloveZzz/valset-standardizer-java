package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * ODS 原始行数据视图。
 */
@Data
@Builder
public class RawValuationRowDTO {
    /**
     * 行号。
     */
    private Integer rowDataNumber;
    /**
     * 行数据。
     */
    private List<Object> rowData;
}
