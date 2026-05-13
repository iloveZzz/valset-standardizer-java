package com.yss.valset.transfer.domain.form.model;

/**
 * 表单尺寸。
 */
public enum FormSize implements YssFormilyDsl.WireValue {
    SMALL("small"),
    MIDDLE("middle"),
    LARGE("large");

    private final String value;

    FormSize(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
