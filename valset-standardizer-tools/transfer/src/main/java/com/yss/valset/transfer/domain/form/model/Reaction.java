package com.yss.valset.transfer.domain.form.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 联动反应。
 */
@Getter
public class Reaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> dependencies = new ArrayList<>();
    private final Map<String, Object> fulfillState = new LinkedHashMap<>();
    private final Map<String, Object> otherwiseState = new LinkedHashMap<>();
    private String whenExpression;
    private String runExpression;

    public Reaction dependency(String dependency) {
        this.dependencies.add(dependency);
        return this;
    }

    public Reaction dependencies(String... dependencies) {
        this.dependencies.addAll(Arrays.asList(dependencies));
        return this;
    }

    public Reaction when(String whenExpression) {
        this.whenExpression = whenExpression;
        return this;
    }

    public Reaction run(String runExpression) {
        this.runExpression = runExpression;
        return this;
    }

    public Reaction fulfillState(String key, Object value) {
        this.fulfillState.put(key, unwrap(value));
        return this;
    }

    public Reaction otherwiseState(String key, Object value) {
        this.otherwiseState.put(key, unwrap(value));
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        YssFormilyDsl.putIfNotEmpty(result, "dependencies", dependencies);
        YssFormilyDsl.putIfNotNull(result, "when", whenExpression);
        YssFormilyDsl.putIfNotNull(result, "run", runExpression);
        if (!fulfillState.isEmpty()) {
            Map<String, Object> fulfill = new LinkedHashMap<>();
            fulfill.put("state", fulfillState);
            result.put("fulfill", fulfill);
        }
        if (!otherwiseState.isEmpty()) {
            Map<String, Object> otherwise = new LinkedHashMap<>();
            otherwise.put("state", otherwiseState);
            result.put("otherwise", otherwise);
        }
        return result;
    }

    private static Object unwrap(Object value) {
        if (value instanceof YssFormilyDsl.WireValue) {
            return ((YssFormilyDsl.WireValue) value).wireValue();
        }
        return value;
    }
}
