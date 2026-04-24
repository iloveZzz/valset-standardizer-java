package com.yss.valset.transfer.domain.form.model;

/**
 * 字段效果类型。
 */
public enum FieldEffectType implements YssFormilyDsl.WireValue {
    ON_FIELD_VALUE_CHANGE("onFieldValueChange");

    private final String value;

    FieldEffectType(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
