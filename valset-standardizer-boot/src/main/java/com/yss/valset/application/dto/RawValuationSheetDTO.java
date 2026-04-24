package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * ODS 原始 sheet 元数据视图。
 */
@Data
@Builder
public class RawValuationSheetDTO {
    /**
     * 工作表名称。
     */
    private String sheetName;
    /**
     * 表头元数据。
     */
    private Map<String, Object> headerMeta;
}
