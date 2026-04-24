package com.yss.valset.transfer.domain.form.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YssFormily 根 schema。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YssSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型，固定为 object。
     */
    private final String type = SchemaType.OBJECT.wireValue().toString();

    /**
     * 属性节点映射。
     */
    private final Map<String, YssSchemaNode> properties = new LinkedHashMap<>();

    public YssSchema property(String name, YssSchemaNode node) {
        this.properties.put(name, node);
        return this;
    }
}
