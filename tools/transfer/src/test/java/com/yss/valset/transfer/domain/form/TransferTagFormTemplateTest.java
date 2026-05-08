package com.yss.valset.transfer.domain.form;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferTagFormTemplateTest {

    @Test
    void shouldExposeDefaultTagFieldInTemplate() {
        TransferTagFormTemplate template = new TransferTagFormTemplate();

        assertThat(template.getName()).isEqualTo(TransferFormTemplateNames.TRANSFER_TAG);
        assertThat(template.initialValues().get("defaultTag")).isEqualTo(Boolean.FALSE);

        Map<String, ?> grid = template.buildForm().getSchema().getProperties()
                .get("layout").getProperties().get("grid").getProperties();
        assertThat(grid).containsKey("defaultTag");

        com.yss.valset.transfer.domain.form.model.YssSchemaNode defaultTagField =
                (com.yss.valset.transfer.domain.form.model.YssSchemaNode) grid.get("defaultTag");
        assertThat(defaultTagField.getXDisabled()).isEqualTo("{{ $values.tagCode !== 'BUSINESS_DATE' }}");
        assertThat(defaultTagField.getXDecoratorProps()).containsEntry("tooltip", "默认标签固定为启用状态，保存后不可编辑或删除");
    }
}
