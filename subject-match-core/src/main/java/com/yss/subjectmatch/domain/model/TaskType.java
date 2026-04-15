package com.yss.subjectmatch.domain.model;

/**
 * 支持的任务类型。
 */
public enum TaskType {
    PARSE_WORKBOOK,
    MATCH_SUBJECT,
    EVALUATE_MAPPING,
    EXPORT_RESULT,
    REFRESH_STANDARD_SUBJECT,
    REFRESH_MAPPING_HINT,
    EXTRACT_DATA
}
