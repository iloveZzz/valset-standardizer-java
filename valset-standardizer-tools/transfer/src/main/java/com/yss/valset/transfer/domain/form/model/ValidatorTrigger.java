package com.yss.valset.transfer.domain.form.model;

/**
 * 验证触发器。
 */
public enum ValidatorTrigger implements YssFormilyDsl.WireValue {
    ON_BLUR("onBlur"),
    ON_CHANGE("onChange"),
    ON_INPUT("onInput");

    private final String value;

    ValidatorTrigger(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
