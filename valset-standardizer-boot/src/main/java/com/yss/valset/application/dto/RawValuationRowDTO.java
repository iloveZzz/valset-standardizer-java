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
    private Integer rowDataNumber;
    private List<Object> rowData;
}
