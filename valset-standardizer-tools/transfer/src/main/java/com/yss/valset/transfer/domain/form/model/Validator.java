package com.yss.valset.transfer.domain.form.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证器。
 */
@Getter
public class Validator implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public static Validator required(String message) {
        Validator validator = new Validator();
        validator.attributes.put("required", true);
        YssFormilyDsl.putIfNotNull(validator.attributes, "message", message);
        return validator;
    }

    public static Validator async(String triggerType, String expression, String message) {
        Validator validator = new Validator();
        validator.attributes.put("triggerType", triggerType);
        validator.attributes.put("validator", expression);
        YssFormilyDsl.putIfNotNull(validator.attributes, "message", message);
        validator.attributes.put("async", true);
        return validator;
    }

    public Validator pattern(String pattern) {
        attributes.put("pattern", pattern);
        return this;
    }

    public Validator max(int max) {
        attributes.put("max", max);
        return this;
    }

    public Validator min(int min) {
        attributes.put("min", min);
        return this;
    }

    public Validator message(String message) {
        attributes.put("message", message);
        return this;
    }

    public Map<String, Object> toMap() {
        return attributes;
    }
}
