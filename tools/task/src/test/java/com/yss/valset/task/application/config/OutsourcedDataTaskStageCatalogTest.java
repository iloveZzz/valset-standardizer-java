package com.yss.valset.task.application.config;

import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 估值表解析任务阶段配置目录测试。
 */
class OutsourcedDataTaskStageCatalogTest {

    @Test
    void shouldResolveDefaultAndConfiguredWorkflowRules() {
        OutsourcedDataTaskStageCatalog catalog = new OutsourcedDataTaskStageCatalog();

        assertThat(catalog.ignoreWorkflowTaskType(TaskType.PARSE_WORKBOOK)).isTrue();
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.SUCCESS)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RUNNING)).isEqualTo(OutsourcedDataTaskStatus.RUNNING);
        assertThat(catalog.resolveParseStepStatus(ParseLifecycleStage.TASK_STANDARDIZED)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.statusLabel("SUCCESS")).isEqualTo("已完成");

        catalog.setIgnoredWorkflowTaskTypes(List.of());
        catalog.setSuccessTaskStatuses(List.of(TaskStatus.RETRYING.name()));
        catalog.setRunningTaskStatuses(List.of(TaskStatus.RUNNING.name()));
        catalog.setStoppedTaskStatuses(List.of(TaskStatus.CANCELED.name()));
        catalog.setFailedTaskStatuses(List.of(TaskStatus.FAILED.name()));

        assertThat(catalog.ignoreWorkflowTaskType(TaskType.PARSE_WORKBOOK)).isFalse();
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RETRYING)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RUNNING)).isEqualTo(OutsourcedDataTaskStatus.RUNNING);
    }
}
