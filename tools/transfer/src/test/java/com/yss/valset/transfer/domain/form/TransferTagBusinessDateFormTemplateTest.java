package com.yss.valset.transfer.domain.form;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferTagBusinessDateFormTemplateTest {

    @Test
    void shouldBuildBusinessDateTemplateWithDateRegexDefaults() {
        TransferTagBusinessDateFormTemplate template = new TransferTagBusinessDateFormTemplate();

        assertThat(template.getName()).isEqualTo(TransferFormTemplateNames.TRANSFER_TAG_BUSINESS_DATE);
        assertThat(template.getDescription()).contains("业务日期提取");

        Map<String, Object> initialValues = template.initialValues();
        assertThat(initialValues.get("tagCode")).isEqualTo("BUSINESS_DATE");
        assertThat(initialValues.get("tagName")).isEqualTo("业务日期");
        assertThat(initialValues.get("tagValue")).isEqualTo("DATE");
        assertThat(initialValues.get("defaultTag")).isEqualTo(Boolean.TRUE);
        assertThat(initialValues.get("matchStrategy")).isEqualTo("REGEX_RULE");
        assertThat(String.valueOf(initialValues.get("regexPattern")))
                .contains("\\d{4}[-/]\\d{2}[-/]\\d{2}")
                .contains("\\d{8}")
                .contains("\\d{4}年\\d{1,2}月\\d{1,2}日");

        @SuppressWarnings("unchecked")
        Map<String, Object> tagMeta = (Map<String, Object>) initialValues.get("tagMeta");
        assertThat(tagMeta).containsEntry("defaultTag", Boolean.TRUE);
        assertThat(tagMeta).containsEntry("candidateFields", List.of("fileName", "originalName", "subject", "path"));
        assertThat(tagMeta).containsKey("supportedFormats");

        var formDefinition = template.buildForm();
        assertThat(formDefinition.getSchema().getProperties()).containsKey("layout");
        Map<String, ?> grid = formDefinition.getSchema().getProperties().get("layout").getProperties().get("grid").getProperties();
        assertThat(grid)
                .containsKeys("businessDateGuide", "tagCode", "tagName", "tagValue", "defaultTag", "regexPattern", "tagMeta");

        com.yss.valset.transfer.domain.form.model.YssSchemaNode defaultTagField =
                (com.yss.valset.transfer.domain.form.model.YssSchemaNode) grid.get("defaultTag");
        assertThat(defaultTagField.getXDisabled()).isEqualTo("{{ true }}");
        assertThat(defaultTagField.getXDecoratorProps()).containsEntry("tooltip", "业务日期标签默认启用且固定为默认标签");
    }
}
