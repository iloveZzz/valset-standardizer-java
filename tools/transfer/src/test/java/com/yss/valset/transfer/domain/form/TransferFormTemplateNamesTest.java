package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.model.TargetType;
import org.junit.jupiter.api.Test;

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
    void local_directory_target_template_should_expose_directory_and_optional_subpath() {
        TransferTargetLocalFormTemplate template = new TransferTargetLocalFormTemplate();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> layoutNode = template.buildForm().getSchema().getProperties().get("layout").toMap();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridNode = (java.util.Map<String, Object>) ((java.util.Map<String, Object>) layoutNode.get("properties")).get("grid");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridProperties = (java.util.Map<String, Object>) gridNode.get("properties");

        assertThat(gridProperties).containsKeys("directory", "targetPathTemplate", "createParentDirectories");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> directoryNode = (java.util.Map<String, Object>) gridProperties.get("directory");
        assertThat(directoryNode).containsEntry("title", "目标目录根路径");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> pathNode = (java.util.Map<String, Object>) gridProperties.get("targetPathTemplate");
        assertThat(pathNode).containsEntry("title", "目标子路径模板");
    }

    @Test
    void should_use_target_path_template_for_filesys_target_template() {
        TransferTargetFilesysFormTemplate template = new TransferTargetFilesysFormTemplate();

        assertThat(template.buildForm().getSchema().getProperties())
                .containsKey("layout");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> layoutNode = template.buildForm().getSchema().getProperties().get("layout").toMap();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridNode = (java.util.Map<String, Object>) ((java.util.Map<String, Object>) layoutNode.get("properties")).get("grid");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridProperties = (java.util.Map<String, Object>) gridNode.get("properties");

        assertThat(gridProperties).containsKey("targetPathTemplate");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> pathNode = (java.util.Map<String, Object>) gridProperties.get("targetPathTemplate");
        assertThat(pathNode).containsEntry("title", "目标路径模板");
    }

    @Test
    void should_not_contain_grouping_fields_in_transfer_rule_template() {
        TransferRuleFormTemplate template = new TransferRuleFormTemplate();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> layoutNode = template.buildForm().getSchema().getProperties().get("layout").toMap();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridNode = (java.util.Map<String, Object>) ((java.util.Map<String, Object>) layoutNode.get("properties")).get("grid");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridProperties = (java.util.Map<String, Object>) gridNode.get("properties");

        assertThat(gridProperties)
                .doesNotContainKeys("groupStrategy", "groupField", "groupTargetMapping", "groupExpression", "regGroup");
    }

    @Test
    void should_not_contain_grouping_fields_in_transfer_route_template() {
        TransferRouteFormTemplate template = new TransferRouteFormTemplate();

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> layoutNode = template.buildForm().getSchema().getProperties().get("layout").toMap();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridNode = (java.util.Map<String, Object>) ((java.util.Map<String, Object>) layoutNode.get("properties")).get("grid");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> gridProperties = (java.util.Map<String, Object>) gridNode.get("properties");

        assertThat(gridProperties)
                .doesNotContainKeys("groupStrategy", "groupField", "groupTargetMapping", "groupExpression", "defaultTargetCode");
    }
}
