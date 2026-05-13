package com.yss.valset.transfer.domain.form.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class YssFormilyModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表单定义。
     */
    private YssFormDefinition formDefinition;

    /**
     * 初始值。
     */
    @Builder.Default
    private Map<String, Object> initialValues = new LinkedHashMap<>();

    public static YssFormilyModel of(YssFormDefinition formDefinition, Map<String, Object> initialValues) {
        YssFormilyModel model = new YssFormilyModel();
        model.setFormDefinition(formDefinition);
        model.setInitialValues(initialValues);
        return model;
    }

    public void setInitialValues(Map<String, Object> initialValues) {
        this.initialValues = initialValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(initialValues);
    }
}
