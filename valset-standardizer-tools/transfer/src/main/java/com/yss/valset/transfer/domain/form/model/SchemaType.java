package com.yss.valset.transfer.domain.form.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 节点类型。
 */
public enum SchemaType {
    OBJECT("object"),
    VOID("void"),
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array");

    private final String value;

    SchemaType(String value) {
        this.value = value;
    }

    @JsonValue
    public Object wireValue() {
        return value;
    }

    @JsonCreator
    public static SchemaType fromWireValue(String value) {
        if (value == null) {
            return null;
        }
        for (SchemaType schemaType : values()) {
            if (schemaType.value.equals(value)) {
                return schemaType;
            }
        }
        throw new IllegalArgumentException("不支持的节点类型: " + value);
    }
}
