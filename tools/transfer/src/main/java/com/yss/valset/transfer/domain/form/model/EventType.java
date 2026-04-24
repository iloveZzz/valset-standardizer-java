package com.yss.valset.transfer.domain.form.model;

/**
 * 事件类型。
 */
public enum EventType implements YssFormilyDsl.WireValue {
    ON_UPDATE_VALUE("onUpdate:value"),
    ON_CHANGE("onChange"),
    ON_SUBMIT("onSubmit");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
