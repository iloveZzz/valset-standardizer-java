package com.yss.valset.transfer.domain.form.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 选项。
 */
@Getter
public class Option implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Object value;
    private final String label;

    public Option(Object value, String label) {
        this.value = value;
        this.label = label;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", label);
        result.put("value", value);
        return result;
    }
}
