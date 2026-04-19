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
     * 行分类。
     */
    ROW_CLASSIFY,
    /**
     * 字段映射。
     */
    COLUMN_MAP,
    /**
     * 值转换。
     */
    VALUE_TRANSFORM,
    /**
     * 标准化。
     */
    NORMALIZE
}
