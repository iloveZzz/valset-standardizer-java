package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.model.EtlPlatformType;

import java.util.List;

/**
 * 工作流调度平台适配器。
 */
public interface WorkflowSchedulePlatformAdapter {

    EtlPlatformType platformType();

    void validate(WorkflowScheduleDTO schedule);

    WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule);

    WorkflowScheduleDTO updateSchedule(WorkflowScheduleDTO schedule);

    void deleteSchedule(Long projectCode, Long scheduleId);

    void onlineSchedule(Long projectCode, Long scheduleId);

    void offlineSchedule(Long projectCode, Long scheduleId);

    List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule);

    List<String> previewSchedule(WorkflowSchedulePreviewRequest request);
}
