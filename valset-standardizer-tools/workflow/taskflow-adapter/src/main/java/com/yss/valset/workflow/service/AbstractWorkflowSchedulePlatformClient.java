package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.spi.WorkflowSchedulePlatformClient;

import java.util.List;

/**
 * 工作流调度平台客户端基类。
 */
public abstract class AbstractWorkflowSchedulePlatformClient implements WorkflowSchedulePlatformClient {

    @Override
    public WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule) {
        return schedule;
    }

    @Override
    public WorkflowScheduleDTO updateSchedule(WorkflowScheduleDTO schedule) {
        return schedule;
    }

    @Override
    public void deleteSchedule(Long projectCode, Long scheduleId) {
    }

    @Override
    public void onlineSchedule(Long projectCode, Long scheduleId) {
    }

    @Override
    public void offlineSchedule(Long projectCode, Long scheduleId) {
    }

    @Override
    public List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule) {
        return List.of();
    }

    @Override
    public List<String> previewSchedule(WorkflowSchedulePreviewRequest request) {
        return List.of();
    }

    @Override
    public abstract EtlPlatformType platformType();

    @Override
    public abstract void validate(WorkflowScheduleDTO schedule);
}
