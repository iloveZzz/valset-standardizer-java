package com.yss.valset.task.application.impl.management;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认估值表解析任务管理应用服务测试。
 */
class DefaultOutsourcedDataTaskManagementAppServiceTest {

    private final DefaultOutsourcedDataTaskManagementAppService service = new DefaultOutsourcedDataTaskManagementAppService();

    @Test
    void shouldReturnSummaryWithAllStages() {
        OutsourcedDataTaskSummaryDTO summary = service.summary(new OutsourcedDataTaskQueryCommand());

        assertThat(summary.getTotalCount()).isGreaterThan(0);
        assertThat(summary.getStepSummaries()).hasSize(OutsourcedDataTaskStage.values().length - 1);
    }

    @Test
    void shouldPageTasksByStage() {
        OutsourcedDataTaskQueryCommand query = new OutsourcedDataTaskQueryCommand();
        query.setStage(OutsourcedDataTaskStage.STANDARD_LANDING.name());
        query.setPageIndex(1);
        query.setPageSize(10);

        PageResult<OutsourcedDataTaskBatchDTO> page = service.pageTasks(query);

        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().get(0).getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
    }

    @Test
    void shouldListStepsByBusinessOrder() {
        List<OutsourcedDataTaskStepDTO> steps = service.listSteps("BATCH-20250227-001");

        assertThat(steps).hasSize(OutsourcedDataTaskStage.values().length - 1);
        assertThat(steps.get(0).getStage()).isEqualTo(OutsourcedDataTaskStage.FILE_PARSE.name());
        assertThat(steps.get(steps.size() - 1).getStage()).isEqualTo(OutsourcedDataTaskStage.VERIFY_ARCHIVE.name());
    }
}
