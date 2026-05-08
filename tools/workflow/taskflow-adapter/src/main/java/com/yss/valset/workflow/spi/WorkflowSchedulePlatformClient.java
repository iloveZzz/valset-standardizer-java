package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;

import java.util.List;

/**
 * 工作流调度平台客户端。
 */
public interface WorkflowSchedulePlatformClient {

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
