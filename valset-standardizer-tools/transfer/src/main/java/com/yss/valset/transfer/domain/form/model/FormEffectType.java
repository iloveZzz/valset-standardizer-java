package com.yss.valset.transfer.domain.form.model;

/**
 * 表单效果类型。
 */
public enum FormEffectType implements YssFormilyDsl.WireValue {
    ON_FORM_SUBMIT("onFormSubmit"),
    ON_FORM_SUBMIT_FAILED("onFormSubmitFailed");

    private final String value;

    FormEffectType(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}
