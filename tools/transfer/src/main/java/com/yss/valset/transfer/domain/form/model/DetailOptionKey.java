package com.yss.valset.transfer.domain.form.model;

/**
 * 详情选项键。
 */
public enum DetailOptionKey implements YssFormilyDsl.WireValue {
    COLUMNS("columns"),
    BORDERED("bordered"),
    HIDE_EMPTY("hideEmpty"),
    EMPTY_PLACEHOLDER("emptyPlaceholder"),
    LABEL_WIDTH("labelWidth"),
    RESPONSIVE("responsive"),
    MAX_COLUMNS("maxColumns"),
    MIN_COLUMNS("minColumns"),
    MIN_WIDTH("minWidth");

    private final String value;

    DetailOptionKey(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
