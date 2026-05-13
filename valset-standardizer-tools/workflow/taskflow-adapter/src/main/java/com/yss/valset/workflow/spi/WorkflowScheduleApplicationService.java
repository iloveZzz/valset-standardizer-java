package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;

import java.util.List;

/**
 * 通用 ETL 工作流调度应用服务。
 */
public interface WorkflowScheduleApplicationService {

    WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule);

    WorkflowScheduleDTO updateSchedule(Long scheduleId, WorkflowScheduleDTO schedule);

    void deleteSchedule(Long projectCode, Long scheduleId);

    void onlineSchedule(Long projectCode, Long scheduleId);

    void offlineSchedule(Long projectCode, Long scheduleId);

    List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule);

    List<String> previewSchedule(WorkflowSchedulePreviewRequest request);
}
