package com.yss.valset.transfer.domain.form.model;

import java.io.Serializable;
import java.util.*;

/**
 * Java DSL for building YssFormily form definitions.
 *
 * <p>This SDK models:
 * <ul>
 *   <li>schema body</li>
 *   <li>form container config such as mode, detail-options, scope, effects</li>
 *   <li>typed enums for common schema types, components, events and options</li>
 * </ul>
 */
public final class YssFormilyDsl implements Serializable{

    private YssFormilyDsl() {
    }

    public interface WireValue {
        Object wireValue();
    }

    public static FormDefinitionBuilder form() {
        return new FormDefinitionBuilder();
    }

    public static NodeBuilder input(String name, String title) {
        return field(name, SchemaType.STRING, title, Component.INPUT);
    }

    public static NodeBuilder textArea(String name, String title) {
        return field(name, SchemaType.STRING, title, Component.INPUT_TEXT_AREA);
    }

    public static NodeBuilder inputNumber(String name, String title) {
        return field(name, SchemaType.NUMBER, title, Component.INPUT_NUMBER);
    }

    public static NodeBuilder select(String name, String title) {
        return field(name, SchemaType.STRING, title, Component.SELECT);
    }

    public static NodeBuilder multiSelect(String name, String title) {
        return field(name, SchemaType.ARRAY, title, Component.SELECT).componentProp("mode", "multiple");
    }

    public static NodeBuilder radioGroup(String name, String title) {
        return field(name, SchemaType.STRING, title, Component.RADIO_GROUP);
    }

    public static NodeBuilder switchField(String name, String title) {
        return field(name, SchemaType.BOOLEAN, title, Component.SWITCH);
    }

    public static NodeBuilder datePicker(String name, String title) {
        return field(name, SchemaType.STRING, title, Component.DATE_PICKER);
    }

    public static NodeBuilder rangePicker(String name, String title) {
        return field(name, SchemaType.ARRAY, title, Component.DATE_RANGE_PICKER);
    }

    public static NodeBuilder slot(String name, String title, String slotName) {
        return field(name, SchemaType.VOID, title, Component.SLOT).componentProp("name", slotName);
    }

    public static NodeBuilder groupHeader(String name, String title) {
        return groupHeader(name, title, null);
    }

    public static NodeBuilder groupHeader(String name, String title, String description) {
        YssSchemaNode node = YssSchemaNode.of(SchemaType.VOID)
            .title(title)
            .decorator(Decorator.FORM_ITEM)
            .component(Component.GROUP_HEADER);
        if (description != null) {
            node.componentProp("description", description);
        }
        return new NodeBuilder(name, node);
    }

    public static NodeBuilder submit(String name, String text, String onSubmitExpression) {
        YssSchemaNode node = YssSchemaNode.of(SchemaType.VOID)
            .component(Component.SUBMIT)
            .content(text)
            .componentProp(EventType.ON_SUBMIT.wireValue().toString(), onSubmitExpression);
        return new NodeBuilder(name, node);
    }

    public static NodeBuilder reset(String name, String text) {
        YssSchemaNode node = YssSchemaNode.of(SchemaType.VOID)
            .component(Component.RESET)
            .content(text);
        return new NodeBuilder(name, node);
    }

    public static NodeBuilder autoButtonGroup(String name) {
        YssSchemaNode node = YssSchemaNode.of(SchemaType.VOID)
            .decorator(Decorator.FORM_ITEM)
            .component(Component.AUTO_BUTTON_GROUP);
        return new NodeBuilder(name, node);
    }

    public static NodeBuilder field(String name, SchemaType type, String title, Component component) {
        YssSchemaNode node = YssSchemaNode.of(type)
            .title(title)
            .decorator(Decorator.FORM_ITEM)
            .component(component);
        return new NodeBuilder(name, node);
    }

    public static NodeBuilder field(String name, String type, String title, String component) {
        return new NodeBuilder(name, YssSchemaNode.of(type).title(title).decorator(Decorator.FORM_ITEM.wireValue().toString()).component(component));
    }

    public static Option option(Object value, String label) {
        return new Option(value, label);
    }

    public static EventBinding event(String name, String expression) {
        return new EventBinding(name, expression);
    }

    public static EventBinding event(EventType eventType, String expression) {
        return new EventBinding(eventType.wireValue().toString(), expression);
    }

    public static Validator requiredValidator(String message) {
        return Validator.required(message);
    }

    public static Validator asyncValidator(String triggerType, String expression, String message) {
        return Validator.async(triggerType, expression, message);
    }

    public static Validator asyncValidator(ValidatorTrigger trigger, String expression, String message) {
        return Validator.async(trigger.wireValue().toString(), expression, message);
    }

    public static Reaction reaction() {
        return new Reaction();
    }

    public static DictConfig remoteDict(String dictCode, String optionsScopeKey) {
        return new DictConfig(dictCode, optionsScopeKey);
    }

    public static FieldEffect onFieldValueChange(String fieldPattern, String expression) {
        return FieldEffect.of(FieldEffectType.ON_FIELD_VALUE_CHANGE, fieldPattern, expression);
    }

    public static FormEffect onFormSubmit(String expression) {
        return FormEffect.of(FormEffectType.ON_FORM_SUBMIT, expression);
    }

    public static FormEffect onFormSubmitFailed(String expression) {
        return FormEffect.of(FormEffectType.ON_FORM_SUBMIT_FAILED, expression);
    }

    public static final class FormDefinitionBuilder {
        /** 表单定义对象 */
        public final YssFormDefinition definition;
        /** 布局节点 */
        private final YssSchemaNode layoutNode;
        /** 网格节点 */
        private final YssSchemaNode gridNode;

        public FormDefinitionBuilder() {
            this.definition = new YssFormDefinition();
            this.layoutNode = YssSchemaNode.of(SchemaType.VOID).component(Component.FORM_LAYOUT);
            this.gridNode = YssSchemaNode.of(SchemaType.VOID).component(Component.FORM_GRID);
            this.layoutNode.property("grid", this.gridNode);
            this.definition.getSchema().property("layout", this.layoutNode);

            horizontal(120);
            gridDefaults(3, 1, 260, 16, 0);
        }

        public FormDefinitionBuilder horizontal(int labelWidth) {
            this.layoutNode.componentProp("layout", LayoutType.HORIZONTAL);
            this.layoutNode.componentProp("labelWidth", labelWidth);
            return this;
        }

        public FormDefinitionBuilder vertical() {
            this.layoutNode.componentProp("layout", LayoutType.VERTICAL);
            this.layoutNode.removeComponentProp("labelWidth");
            return this;
        }

        public FormDefinitionBuilder labelAlign(LabelAlign labelAlign) {
            this.layoutNode.componentProp("labelAlign", labelAlign);
            return this;
        }

        public FormDefinitionBuilder labelAlign(String labelAlign) {
            this.layoutNode.componentProp("labelAlign", labelAlign);
            return this;
        }

        public FormDefinitionBuilder size(FormSize size) {
            this.layoutNode.componentProp("size", size);
            return this;
        }

        public FormDefinitionBuilder size(String size) {
            this.layoutNode.componentProp("size", size);
            return this;
        }

        public FormDefinitionBuilder gridDefaults(int maxColumns, int minColumns, int minWidth, int columnGap, int rowGap) {
            this.gridNode.componentProp("maxColumns", maxColumns);
            this.gridNode.componentProp("minColumns", minColumns);
            this.gridNode.componentProp("minWidth", minWidth);
            this.gridNode.componentProp("columnGap", columnGap);
            this.gridNode.componentProp("rowGap", rowGap);
            this.definition.getGridDefaults().put("maxColumns", maxColumns);
            this.definition.getGridDefaults().put("minColumns", minColumns);
            this.definition.getGridDefaults().put("minWidth", minWidth);
            this.definition.getGridDefaults().put("columnGap", columnGap);
            this.definition.getGridDefaults().put("rowGap", rowGap);
            return this;
        }

        public FormDefinitionBuilder mode(Mode mode) {
            this.definition.setMode((Integer) mode.wireValue());
            if (mode == Mode.DETAIL) {
                this.definition.setReadPretty(true);
            }
            return this;
        }

        public FormDefinitionBuilder readPretty(boolean readPretty) {
            this.definition.setReadPretty(readPretty);
            return this;
        }

        public FormDefinitionBuilder initialValues(Map<String, Object> initialValues) {
            this.definition.setInitialValues(initialValues);
            return this;
        }

        public FormDefinitionBuilder modelShape(Map<String, Object> modelShape) {
            this.definition.setModelShape(modelShape);
            return this;
        }

        public FormDefinitionBuilder scopeKey(String scopeKey) {
            this.definition.getScopeKeys().add(scopeKey);
            return this;
        }

        public FormDefinitionBuilder scopeKeys(String... scopeKeys) {
            this.definition.getScopeKeys().addAll(Arrays.asList(scopeKeys));
            return this;
        }

        public FormDefinitionBuilder componentKey(String componentKey) {
            this.definition.getComponentKeys().add(componentKey);
            return this;
        }

        public FormDefinitionBuilder components(String... componentKeys) {
            this.definition.getComponentKeys().addAll(Arrays.asList(componentKeys));
            return this;
        }

        public FormDefinitionBuilder detailOption(DetailOptionKey key, Object value) {
            this.definition.getDetailOptions().put(key.wireValue().toString(), unwrap(value));
            return this;
        }

        public FormDefinitionBuilder detailOption(String key, Object value) {
            this.definition.getDetailOptions().put(key, unwrap(value));
            return this;
        }

        public FormDefinitionBuilder detailOptions(Map<String, Object> detailOptions) {
            this.definition.getDetailOptions().putAll(detailOptions);
            return this;
        }

        public FormDefinitionBuilder effect(FormEffect effect) {
            this.definition.getFormEffects().add(effect);
            return this;
        }

        public FormDefinitionBuilder effects(FormEffect... effects) {
            this.definition.getFormEffects().addAll(Arrays.asList(effects));
            return this;
        }

        public FormDefinitionBuilder fieldEffect(FieldEffect fieldEffect) {
            this.definition.getFieldEffects().add(fieldEffect);
            return this;
        }

        public FormDefinitionBuilder fieldEffects(FieldEffect... fieldEffects) {
            this.definition.getFieldEffects().addAll(Arrays.asList(fieldEffects));
            return this;
        }

        public FormDefinitionBuilder node(NodeBuilder builder) {
            this.gridNode.property(builder.name(), builder.build());
            return this;
        }

        public FormDefinitionBuilder nodes(NodeBuilder... builders) {
            for (NodeBuilder builder : builders) {
                node(builder);
            }
            return this;
        }

        public YssFormDefinition build() {
            return this.definition;
        }

    }

    public static final class NodeBuilder {
        /** 节点名称 */
        private final String name;
        /** 节点对象 */
        private final YssSchemaNode node;

        private NodeBuilder(String name, YssSchemaNode node) {
            this.name = name;
            this.node = node;
        }

        public String name() {
            return name;
        }

        public YssSchemaNode build() {
            return node;
        }

        public NodeBuilder title(String title) {
            node.title(title);
            return this;
        }

        public NodeBuilder required() {
            node.required(true);
            return this;
        }

        public NodeBuilder required(boolean required) {
            node.required(required);
            return this;
        }

        public NodeBuilder decorator(Decorator decorator) {
            node.decorator(decorator);
            return this;
        }

        public NodeBuilder decorator(String decorator) {
            node.decorator(decorator);
            return this;
        }

        public NodeBuilder component(Component component) {
            node.component(component);
            return this;
        }

        public NodeBuilder component(String component) {
            node.component(component);
            return this;
        }

        public NodeBuilder componentProp(String key, Object value) {
            node.componentProp(key, value);
            return this;
        }

        public NodeBuilder decoratorProp(String key, Object value) {
            node.decoratorProp(key, value);
            return this;
        }

        public NodeBuilder placeholder(String placeholder) {
            node.componentProp("placeholder", placeholder);
            return this;
        }

        public NodeBuilder tooltip(String tooltip) {
            node.decoratorProp("tooltip", tooltip);
            return this;
        }

        public NodeBuilder gridSpan(int gridSpan) {
            node.decoratorProp("gridSpan", gridSpan);
            return this;
        }

        public NodeBuilder visibleExpr(String expression) {
            node.xVisible(expression);
            return this;
        }

        public NodeBuilder disabledExpr(String expression) {
            node.xDisabled(expression);
            return this;
        }

        public NodeBuilder reactions(Object reactions) {
            node.xReactions(reactions);
            return this;
        }

        public NodeBuilder reaction(Reaction reaction) {
            node.reaction(reaction);
            return this;
        }

        public NodeBuilder event(String eventName, String expression) {
            node.event(new EventBinding(eventName, expression));
            return this;
        }

        public NodeBuilder event(EventType eventType, String expression) {
            node.event(new EventBinding(eventType.wireValue().toString(), expression));
            return this;
        }

        public NodeBuilder event(EventBinding eventBinding) {
            node.event(eventBinding);
            return this;
        }

        public NodeBuilder validator(Validator validator) {
            node.validator(validator);
            return this;
        }

        public NodeBuilder validators(Validator... validators) {
            node.validators(Arrays.asList(validators));
            return this;
        }

        public NodeBuilder previewFormat(String expression) {
            node.previewFormat(expression);
            return this;
        }

        public NodeBuilder content(String content) {
            node.content(content);
            return this;
        }

        public NodeBuilder option(Object value, String label) {
            node.option(value, label);
            return this;
        }

        public NodeBuilder options(Option... options) {
            node.options(Arrays.asList(options));
            return this;
        }

        public NodeBuilder enumExpression(String expression) {
            node.setEnum(expression);
            return this;
        }

        public NodeBuilder remoteDict(DictConfig dictConfig) {
            node.remoteDict(dictConfig);
            return this;
        }

        public NodeBuilder property(String childName, YssSchemaNode childNode) {
            node.property(childName, childNode);
            return this;
        }
    }

    static Map<String, Object> convertProperties(Map<String, YssSchemaNode> properties) {
        if (properties == null || properties.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, YssSchemaNode> entry : properties.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toMap());
        }
        return result;
    }

    static Object unwrap(Object value) {
        if (value instanceof WireValue) {
            return ((WireValue) value).wireValue();
        }
        return value;
    }

    static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    static void putIfNotEmpty(Map<String, Object> map, String key, Map<String, Object> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    static void putIfNotEmpty(Map<String, Object> map, String key, List<?> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }
}
