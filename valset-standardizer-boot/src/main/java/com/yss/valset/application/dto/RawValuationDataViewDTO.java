package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * ODS 原始数据查询结果。
 */
@Data
@Builder
public class RawValuationDataViewDTO {
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 原始行总数。
     */
    private Integer totalRows;
    /**
     * 原始 sheet 列表。
     */
    private List<RawValuationSheetDTO> sheets;
    /**
     * 原始行明细。
     */
    private List<RawValuationRowDTO> rows;
}
