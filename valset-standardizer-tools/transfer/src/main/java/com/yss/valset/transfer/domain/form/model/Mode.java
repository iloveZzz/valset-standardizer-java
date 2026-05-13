package com.yss.valset.transfer.domain.form.model;

/**
 * 表单模式。
 */
public enum Mode implements YssFormilyDsl.WireValue {
    CREATE(0),
    EDIT(1),
    DETAIL(2);

    private final Integer code;

    Mode(Integer code) {
        this.code = code;
    }

    @Override
    public Object wireValue() {
        return code;
    }
}
