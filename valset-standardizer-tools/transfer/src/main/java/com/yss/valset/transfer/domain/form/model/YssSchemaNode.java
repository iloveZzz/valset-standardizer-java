package com.yss.valset.transfer.domain.form.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YssFormily schema 节点。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YssSchemaNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 节点类型 */
    private SchemaType type;
    /** 节点标题 */
    private String title;
    /** 是否必填 */
    private Boolean required;
    /** 组件类型 */
    @JsonProperty("x-component")
    private String xComponent;
    /** 装饰器类型 */
    @JsonProperty("x-decorator")
    private String xDecorator;
    /** 组件属性 */
    @JsonProperty("x-component-props")
    private final Map<String, Object> xComponentProps = new LinkedHashMap<>();
    /** 装饰器属性 */
    @JsonProperty("x-decorator-props")
    private final Map<String, Object> xDecoratorProps = new LinkedHashMap<>();
    /** 可见性控制 */
    @JsonProperty("x-visible")
    private Object xVisible;
    /** 禁用控制 */
    @JsonProperty("x-disabled")
    private Object xDisabled;
    /** 联动反应 */
    @JsonProperty("x-reactions")
    private Object xReactions;
    /** 枚举选项 */
    @JsonProperty("enum")
    private Object xEnum;
    /** 验证器 */
    @JsonProperty("x-validator")
    private Object xValidator;
    /** 预览格式化表达式 */
    @JsonProperty("x-preview-format")
    private String xPreviewFormat;
    /** 内容 */
    @JsonProperty("x-content")
    private String xContent;
    /** 子属性节点映射 */
    private final Map<String, YssSchemaNode> properties = new LinkedHashMap<>();
    /** 字典配置 */
    private DictConfig dictConfig;

    public static YssSchemaNode of(SchemaType type) {
        YssSchemaNode node = new YssSchemaNode();
        node.type = type;
        return node;
    }

    public static YssSchemaNode of(String type) {
        return of(parseType(type));
    }

    public YssSchemaNode title(String title) {
        this.title = title;
        return this;
    }

    public YssSchemaNode required(Boolean required) {
        this.required = required;
        return this;
    }

    public YssSchemaNode component(Component xComponent) {
        this.xComponent = xComponent.wireValue().toString();
        return this;
    }

    public YssSchemaNode component(String xComponent) {
        this.xComponent = xComponent;
        return this;
    }

    public YssSchemaNode decorator(Decorator xDecorator) {
        this.xDecorator = xDecorator.wireValue().toString();
        return this;
    }

    public YssSchemaNode decorator(String xDecorator) {
        this.xDecorator = xDecorator;
        return this;
    }

    public YssSchemaNode componentProp(String key, Object value) {
        this.xComponentProps.put(key, unwrap(value));
        return this;
    }

    public YssSchemaNode removeComponentProp(String key) {
        this.xComponentProps.remove(key);
        return this;
    }

    public YssSchemaNode decoratorProp(String key, Object value) {
        this.xDecoratorProps.put(key, unwrap(value));
        return this;
    }

    public YssSchemaNode xVisible(Object xVisible) {
        this.xVisible = unwrap(xVisible);
        return this;
    }

    public YssSchemaNode xDisabled(Object xDisabled) {
        this.xDisabled = unwrap(xDisabled);
        return this;
    }

    public YssSchemaNode xReactions(Object xReactions) {
        this.xReactions = unwrap(xReactions);
        return this;
    }

    public YssSchemaNode reaction(Reaction reaction) {
        if (!(xReactions instanceof List<?>)) {
            xReactions = new ArrayList<Map<String, Object>>();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reactions = (List<Map<String, Object>>) xReactions;
        reactions.add(reaction.toMap());
        return this;
    }

    public YssSchemaNode event(EventBinding eventBinding) {
        this.xComponentProps.put(eventBinding.getName(), eventBinding.getExpression());
        return this;
    }

    public YssSchemaNode validator(Validator validator) {
        if (!(xValidator instanceof List<?>)) {
            xValidator = new ArrayList<Map<String, Object>>();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validators = (List<Map<String, Object>>) xValidator;
        validators.add(validator.toMap());
        return this;
    }

    public YssSchemaNode validators(List<Validator> validators) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Validator validator : validators) {
            result.add(validator.toMap());
        }
        this.xValidator = result;
        return this;
    }

    public YssSchemaNode previewFormat(String expression) {
        this.xPreviewFormat = expression;
        return this;
    }

    public YssSchemaNode setEnum(Object xEnum) {
        this.xEnum = unwrap(xEnum);
        return this;
    }

    public YssSchemaNode content(String xContent) {
        this.xContent = xContent;
        return this;
    }

    public YssSchemaNode option(Object value, String label) {
        if (!(xEnum instanceof List<?>)) {
            xEnum = new ArrayList<Map<String, Object>>();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) xEnum;
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("label", label);
        option.put("value", value);
        options.add(option);
        return this;
    }

    public YssSchemaNode options(List<Option> options) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (Option option : options) {
            values.add(option.toMap());
        }
        this.xEnum = values;
        return this;
    }

    public YssSchemaNode remoteDict(DictConfig dictConfig) {
        this.dictConfig = dictConfig;
        this.xEnum = dictConfig.toEnumExpression();
        if (dictConfig.getPreviewFormatExpression() != null) {
            this.xPreviewFormat = dictConfig.getPreviewFormatExpression();
        }
        return this;
    }

    public YssSchemaNode property(String name, YssSchemaNode node) {
        this.properties.put(name, node);
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        YssFormilyDsl.putIfNotNull(result, "type", type == null ? null : type.wireValue());
        YssFormilyDsl.putIfNotNull(result, "title", title);
        YssFormilyDsl.putIfNotNull(result, "required", required);
        YssFormilyDsl.putIfNotNull(result, "x-component", xComponent);
        YssFormilyDsl.putIfNotNull(result, "x-decorator", xDecorator);
        YssFormilyDsl.putIfNotEmpty(result, "x-component-props", xComponentProps);
        YssFormilyDsl.putIfNotEmpty(result, "x-decorator-props", xDecoratorProps);
        YssFormilyDsl.putIfNotNull(result, "x-visible", xVisible);
        YssFormilyDsl.putIfNotNull(result, "x-disabled", xDisabled);
        YssFormilyDsl.putIfNotNull(result, "x-reactions", xReactions);
        YssFormilyDsl.putIfNotNull(result, "enum", xEnum);
        YssFormilyDsl.putIfNotNull(result, "x-validator", xValidator);
        YssFormilyDsl.putIfNotNull(result, "x-preview-format", xPreviewFormat);
        YssFormilyDsl.putIfNotNull(result, "x-content", xContent);
        if (dictConfig != null) {
            result.put("x-yss-dict", dictConfig.toMap());
        }
        if (!properties.isEmpty()) {
            result.put("properties", YssFormilyDsl.convertProperties(properties));
        }
        return result;
    }

    private static Object unwrap(Object value) {
        if (value instanceof YssFormilyDsl.WireValue) {
            return ((YssFormilyDsl.WireValue) value).wireValue();
        }
        return value;
    }

    private static SchemaType parseType(String type) {
        if (type == null) {
            return null;
        }
        for (SchemaType schemaType : SchemaType.values()) {
            if (schemaType.wireValue().toString().equals(type)) {
                return schemaType;
            }
        }
        throw new IllegalArgumentException("不支持的节点类型: " + type);
    }
}
