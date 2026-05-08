package com.yss.valset.workflow.web;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.spi.WorkflowScheduleApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通用 ETL 工作流调度接口。
 */
@RestController
@RequestMapping("/api/etl/schedules")
public class WorkflowScheduleController {

    private final WorkflowScheduleApplicationService workflowScheduleApplicationService;

    public WorkflowScheduleController(WorkflowScheduleApplicationService workflowScheduleApplicationService) {
        this.workflowScheduleApplicationService = workflowScheduleApplicationService;
    }

    @PostMapping
    public SingleResult<WorkflowScheduleDTO> saveSchedule(@Valid @RequestBody WorkflowScheduleDTO request) {
        return SingleResult.of(workflowScheduleApplicationService.saveSchedule(request));
    }

    @PutMapping("/{scheduleId}")
    public SingleResult<WorkflowScheduleDTO> updateSchedule(@PathVariable Long scheduleId,
                                                            @Valid @RequestBody WorkflowScheduleDTO request) {
        return SingleResult.of(workflowScheduleApplicationService.updateSchedule(scheduleId, request));
    }

    @DeleteMapping("/{projectCode}/{scheduleId}")
    public SingleResult<Boolean> deleteSchedule(@PathVariable Long projectCode,
                                                 @PathVariable Long scheduleId) {
        workflowScheduleApplicationService.deleteSchedule(projectCode, scheduleId);
        return SingleResult.of(true);
    }

    @PostMapping("/{projectCode}/{scheduleId}/online")
    public SingleResult<Boolean> onlineSchedule(@PathVariable Long projectCode,
                                                @PathVariable Long scheduleId) {
        workflowScheduleApplicationService.onlineSchedule(projectCode, scheduleId);
        return SingleResult.of(true);
    }

    @PostMapping("/{projectCode}/{scheduleId}/offline")
    public SingleResult<Boolean> offlineSchedule(@PathVariable Long projectCode,
                                                 @PathVariable Long scheduleId) {
        workflowScheduleApplicationService.offlineSchedule(projectCode, scheduleId);
        return SingleResult.of(true);
    }

    @GetMapping
    public MultiResult<WorkflowScheduleDTO> querySchedules(@RequestParam String workflowCode,
                                                           @RequestParam Integer workflowVersionNo,
                                                           @RequestParam(required = false) String searchVal) {
        WorkflowScheduleDTO filter = WorkflowScheduleDTO.builder()
                .workflowCode(workflowCode)
                .workflowVersionNo(workflowVersionNo)
                .searchVal(searchVal)
                .build();
        List<WorkflowScheduleDTO> schedules = workflowScheduleApplicationService.querySchedules(filter);
        return MultiResult.of(schedules);
    }

    @PostMapping("/preview")
    public MultiResult<String> previewSchedule(@Valid @RequestBody WorkflowSchedulePreviewRequest request) {
        return MultiResult.of(workflowScheduleApplicationService.previewSchedule(request));
    }
}
