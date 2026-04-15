package com.yss.subjectmatch.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * ODS 原始 sheet 元数据视图。
 */
@Data
@Builder
public class RawValuationSheetDTO {
    private String sheetName;
    private Map<String, Object> headerMeta;
}
