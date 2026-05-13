package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.spi.WorkflowSchedulePlatformAdapter;
import com.yss.valset.workflow.spi.WorkflowSchedulePlatformClient;

import java.util.List;

/**
 * 平台调度适配器委派壳。
 */
public abstract class AbstractWorkflowSchedulePlatformAdapter implements WorkflowSchedulePlatformAdapter {

    private final WorkflowSchedulePlatformClient client;

    protected AbstractWorkflowSchedulePlatformAdapter(WorkflowSchedulePlatformClient client) {
        this.client = client;
    }

    @Override
    public EtlPlatformType platformType() {
        return client.platformType();
    }

    @Override
    public void validate(WorkflowScheduleDTO schedule) {
        client.validate(schedule);
    }

    @Override
    public WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule) {
        return client.saveSchedule(schedule);
    }

    @Override
    public WorkflowScheduleDTO updateSchedule(WorkflowScheduleDTO schedule) {
        return client.updateSchedule(schedule);
    }

    @Override
    public void deleteSchedule(Long projectCode, Long scheduleId) {
        client.deleteSchedule(projectCode, scheduleId);
    }

    @Override
    public void onlineSchedule(Long projectCode, Long scheduleId) {
        client.onlineSchedule(projectCode, scheduleId);
    }

    @Override
    public void offlineSchedule(Long projectCode, Long scheduleId) {
        client.offlineSchedule(projectCode, scheduleId);
    }

    @Override
    public List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule) {
        return client.querySchedules(schedule);
    }

    @Override
    public List<String> previewSchedule(WorkflowSchedulePreviewRequest request) {
        return client.previewSchedule(request);
    }
}
