package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.model.WorkflowErrorCode;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import com.yss.valset.workflow.spi.WorkflowScheduleApplicationService;
import com.yss.valset.workflow.spi.WorkflowSchedulePlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 默认通用 ETL 工作流调度应用服务。
 */
@Slf4j
@Service
public class DefaultWorkflowScheduleApplicationService implements WorkflowScheduleApplicationService {

    private final WorkflowRuntimeStore runtimeStore;
    private final List<WorkflowSchedulePlatformAdapter> adapters;

    public DefaultWorkflowScheduleApplicationService(WorkflowRuntimeStore runtimeStore,
                                                     List<WorkflowSchedulePlatformAdapter> adapters) {
        this.runtimeStore = runtimeStore;
        this.adapters = adapters == null ? java.util.Arrays.asList() : adapters;
    }

    @Override
    public WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule) {
        WorkflowDefinitionDTO definition = loadDefinition(schedule);
        WorkflowScheduleDTO normalized = normalizeSchedule(definition, schedule);
        resolveAdapter(definition.getPlatformType()).validate(normalized);
        return resolveAdapter(definition.getPlatformType()).saveSchedule(normalized);
    }

    @Override
    public WorkflowScheduleDTO updateSchedule(Long scheduleId, WorkflowScheduleDTO schedule) {
        WorkflowDefinitionDTO definition = loadDefinition(schedule);
        WorkflowScheduleDTO normalized = normalizeSchedule(definition, schedule.toBuilder()
                .scheduleId(scheduleId)
                .build());
        resolveAdapter(definition.getPlatformType()).validate(normalized);
        return resolveAdapter(definition.getPlatformType()).updateSchedule(normalized);
    }

    @Override
    public void deleteSchedule(Long projectCode, Long scheduleId) {
        resolveAnyAdapter().deleteSchedule(projectCode, scheduleId);
    }

    @Override
    public void onlineSchedule(Long projectCode, Long scheduleId) {
        resolveAnyAdapter().onlineSchedule(projectCode, scheduleId);
    }

    @Override
    public void offlineSchedule(Long projectCode, Long scheduleId) {
        resolveAnyAdapter().offlineSchedule(projectCode, scheduleId);
    }

    @Override
    public List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule) {
        WorkflowDefinitionDTO definition = loadDefinition(schedule);
        WorkflowScheduleDTO normalized = normalizeSchedule(definition, schedule);
        return resolveAdapter(definition.getPlatformType()).querySchedules(normalized);
    }

    @Override
    public List<String> previewSchedule(WorkflowSchedulePreviewRequest request) {
        return resolveAnyAdapter().previewSchedule(request);
    }

    private WorkflowDefinitionDTO loadDefinition(WorkflowScheduleDTO schedule) {
        if (schedule == null || !StringUtils.hasText(schedule.getWorkflowCode()) || schedule.getWorkflowVersionNo() == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_WORKFLOW_DEFINITION.getMessage());
        }
        return runtimeStore.findDefinition(schedule.getWorkflowCode(), schedule.getWorkflowVersionNo())
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
    }

    private WorkflowScheduleDTO normalizeSchedule(WorkflowDefinitionDTO definition, WorkflowScheduleDTO schedule) {
        WorkflowScheduleDTO normalized = schedule == null ? WorkflowScheduleDTO.builder().build() : schedule.toBuilder().build();
        if (normalized.getWorkflowCode() == null) {
            normalized.setWorkflowCode(definition.getWorkflowCode());
        }
        if (normalized.getWorkflowVersionNo() == null) {
            normalized.setWorkflowVersionNo(definition.getWorkflowVersionNo());
        }
        if (normalized.getProjectCode() == null && definition.getEngineBinding() != null) {
            normalized.setProjectCode(parseLong(definition.getEngineBinding().getExternalProjectCode()));
        }
        if (normalized.getWorkflowDefinitionCode() == null) {
            normalized.setWorkflowDefinitionCode(parseLong(definition.getEngineBinding() == null ? null : definition.getEngineBinding().getExternalWorkflowId()));
        }
        return normalized;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private WorkflowSchedulePlatformAdapter resolveAdapter(EtlPlatformType platformType) {
        return adapters.stream()
                .filter(adapter -> adapter != null && adapter.platformType() == platformType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.ADAPTER_NOT_FOUND.getMessage() + "：" + platformType));
    }

    private WorkflowSchedulePlatformAdapter resolveAnyAdapter() {
        return adapters.stream()
                .filter(adapter -> adapter != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.ADAPTER_NOT_FOUND.getMessage()));
    }
}
