package com.yss.valset.transfer.domain.form.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字典配置。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DictConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String dictCode;
    private final String optionsScopeKey;
    private String labelKey = "label";
    private String valueKey = "value";
    private String previewFormatExpression;
    private String optionsExpression;

    public DictConfig(String dictCode, String optionsScopeKey) {
        this.dictCode = dictCode;
        this.optionsScopeKey = optionsScopeKey;
    }

    public DictConfig labelKey(String labelKey) {
        this.labelKey = labelKey;
        return this;
    }

    public DictConfig valueKey(String valueKey) {
        this.valueKey = valueKey;
        return this;
    }

    public DictConfig optionsExpression(String optionsExpression) {
        this.optionsExpression = optionsExpression;
        return this;
    }

    public DictConfig previewFormat(String previewFormatExpression) {
        this.previewFormatExpression = previewFormatExpression;
        return this;
    }

    public String toEnumExpression() {
        if (optionsExpression != null) {
            return optionsExpression;
        }
        return "{{ dicts." + optionsScopeKey + " }}";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dictCode", dictCode);
        result.put("optionsScopeKey", optionsScopeKey);
        result.put("labelKey", labelKey);
        result.put("valueKey", valueKey);
        YssFormilyDsl.putIfNotNull(result, "previewFormatExpression", previewFormatExpression);
        YssFormilyDsl.putIfNotNull(result, "optionsExpression", optionsExpression);
        return result;
    }
}
