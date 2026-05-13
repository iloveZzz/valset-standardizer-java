package com.yss.valset.transfer.domain.form.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表单效果。
 */
@Getter
public class FormEffect implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String expression;

    private FormEffect(String type, String expression) {
        this.type = type;
        this.expression = expression;
    }

    public static FormEffect of(FormEffectType type, String expression) {
        return new FormEffect(type.wireValue().toString(), expression);
    }

    public static FormEffect of(String type, String expression) {
        return new FormEffect(type, expression);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("expression", expression);
        return result;
    }
}
