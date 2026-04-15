package com.yss.subjectmatch.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * ODS 原始数据查询结果。
 */
@Data
@Builder
public class RawValuationDataViewDTO {
    private Long fileId;
    private Integer totalRows;
    private List<RawValuationSheetDTO> sheets;
    private List<RawValuationRowDTO> rows;
}
