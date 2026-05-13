package com.yss.valset.transfer.domain.form.model;

/**
 * 装饰器类型。
 */
public enum Decorator implements YssFormilyDsl.WireValue {
    FORM_ITEM("FormItem");

    private final String value;

    Decorator(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
