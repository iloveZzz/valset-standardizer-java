package com.yss.valset.transfer.domain.form.model;

/**
 * 布局类型。
 */
public enum LayoutType implements YssFormilyDsl.WireValue {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");

    private final String value;

    LayoutType(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
