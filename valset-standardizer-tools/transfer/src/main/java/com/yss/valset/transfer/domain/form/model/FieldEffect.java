package com.yss.valset.transfer.domain.form.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段效果。
 */
@Getter
public class FieldEffect implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String fieldPattern;
    private final String expression;

    private FieldEffect(String type, String fieldPattern, String expression) {
        this.type = type;
        this.fieldPattern = fieldPattern;
        this.expression = expression;
    }

    public static FieldEffect of(FieldEffectType type, String fieldPattern, String expression) {
        return new FieldEffect(type.wireValue().toString(), fieldPattern, expression);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("fieldPattern", fieldPattern);
        result.put("expression", expression);
        return result;
    }
}
