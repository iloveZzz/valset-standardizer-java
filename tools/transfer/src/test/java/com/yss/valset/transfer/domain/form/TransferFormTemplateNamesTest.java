package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.model.TargetType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 表单模板名称映射测试。
 */
class TransferFormTemplateNamesTest {

    @Test
    void should_map_local_directory_target_to_local_template() {
        assertThat(TransferFormTemplateNames.targetTemplateName(TargetType.LOCAL_DIR))
                .isEqualTo("transfer_target_local");
    }

    @Test
    void should_use_directory_field_for_filesys_target_template() {
        TransferTargetFilesysFormTemplate template = new TransferTargetFilesysFormTemplate();

        @SuppressWarnings("unchecked")
        Map<String, Object> layoutNode = template.buildForm().getSchema().getProperties().get("layout").toMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> gridNode = (Map<String, Object>) ((Map<String, Object>) layoutNode.get("properties")).get("grid");
        @SuppressWarnings("unchecked")
        Map<String, Object> gridProperties = (Map<String, Object>) gridNode.get("properties");

        assertThat(gridProperties).containsKey("directory");
        @SuppressWarnings("unchecked")
        Map<String, Object> directoryNode = (Map<String, Object>) gridProperties.get("directory");
        assertThat(directoryNode).containsEntry("title", "目录路径");
    }
}
