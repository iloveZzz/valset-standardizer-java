package com.yss.valset.transfer.domain.form.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YssFormily 表单定义。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YssFormDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表单结构。
     */
    public final YssSchema schema = new YssSchema();

    /**
     * 表单模式，0-创建，1-编辑，2-详情。
     */
    public Integer mode = (Integer) Mode.CREATE.wireValue();

    /**
     * 是否只读模式。
     */
    public Boolean readPretty = false;

    /**
     * 初始值。
     */
    public Map<String, Object> initialValues = new LinkedHashMap<>();

    /**
     * 模型结构。
     */
    public Map<String, Object> modelShape = new LinkedHashMap<>();

    /**
     * 详情选项。
     */
    public final Map<String, Object> detailOptions = new LinkedHashMap<>();

    /**
     * 网格默认配置。
     */
    public final Map<String, Object> gridDefaults = new LinkedHashMap<>();

    /**
     * 作用域键列表。
     */
    public final List<String> scopeKeys = new ArrayList<>();

    /**
     * 组件键列表。
     */
    public final List<String> componentKeys = new ArrayList<>();

    /**
     * 表单效果列表。
     */
    public final List<FormEffect> formEffects = new ArrayList<>();

    /**
     * 字段效果列表。
     */
    public final List<FieldEffect> fieldEffects = new ArrayList<>();

    public YssFormDefinition toPojo() {
        return this;
    }
}
