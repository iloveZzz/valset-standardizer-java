package com.yss.valset.transfer.domain.form.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YssFormilyDslTest {

    @Test
    void groupHeaderShouldUseNodeTitleAndKeepDescriptionInComponentProps() {
        YssSchemaNode node = YssFormilyDsl.groupHeader("group", "分组路由配置", "这是一段分组描述信息").build();

        Map<String, Object> map = node.toMap();

        assertThat(map).containsEntry("title", "分组路由配置");
        assertThat(map).containsEntry("x-decorator", "FormItem");
        assertThat(map).containsEntry("x-component", "GroupHeader");
        assertThat(map).containsKey("x-component-props");

        @SuppressWarnings("unchecked")
        Map<String, Object> componentProps = (Map<String, Object>) map.get("x-component-props");
        assertThat(componentProps).containsEntry("description", "这是一段分组描述信息");
        assertThat(componentProps).doesNotContainKey("title");
    }

    @Test
    void schemaTypeShouldSerializeToLowerCaseWireValue() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(YssSchemaNode.of(SchemaType.VOID));

        assertThat(json).contains("\"type\":\"void\"");
        assertThat(json).doesNotContain("\"type\":\"VOID\"");
    }
}
