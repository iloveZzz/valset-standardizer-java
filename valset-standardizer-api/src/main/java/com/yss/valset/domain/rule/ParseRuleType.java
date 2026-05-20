package com.yss.valset.domain.rule;

/**
 * 解析规则类型。
 */
public enum ParseRuleType {
    /**
     * 模板匹配。
     */
    PROFILE_MATCH,
    /**
     * 表头识别。
     */
    HEADER_DETECT,
    /**
     * 数据起始行识别。
     */
    DATA_START,
    /**
     * 行分类。
     */
    ROW_CLASSIFY,
    /**
     * 科目字段抽取。
     */
    SUBJECT_EXTRACT,
    /**
     * 指标字段抽取。
     */
    METRIC_EXTRACT,
    /**
     * 字段映射。
     */
    COLUMN_MAP,
    /**
     * 字段映射。
     */
    FIELD_MAP,
    /**
     * 值转换。
     */
    VALUE_TRANSFORM,
    /**
     * 标准化。
     */
    NORMALIZE
}
