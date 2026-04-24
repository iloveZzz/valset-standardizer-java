package com.yss.valset.transfer.domain.form.model;

/**
 * 标签对齐方式。
 */
public enum LabelAlign implements YssFormilyDsl.WireValue {
    LEFT("left"),
    RIGHT("right");

    private final String value;

    LabelAlign(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
