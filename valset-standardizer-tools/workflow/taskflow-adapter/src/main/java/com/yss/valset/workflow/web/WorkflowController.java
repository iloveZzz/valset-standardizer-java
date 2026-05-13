package com.yss.valset.workflow.web;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.cloud.dto.result.MultiResult;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowTaskInstancePageDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.spi.WorkflowApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用 ETL 工作流接口。
 */
@RestController
@RequestMapping("/etl/workflows")
public class WorkflowController {

    private final WorkflowApplicationService workflowApplicationService;

    public WorkflowController(WorkflowApplicationService workflowApplicationService) {
        this.workflowApplicationService = workflowApplicationService;
    }

    @PostMapping
    public SingleResult<WorkflowDefinitionDTO> saveDefinition(@Valid @RequestBody WorkflowDefinitionDTO request) {
        return SingleResult.of(workflowApplicationService.saveDefinition(request));
    }

    @PostMapping("/{workflowCode}/{workflowVersionNo}/sync")
    public SingleResult<WorkflowDefinitionDTO> syncDefinition(@PathVariable String workflowCode,
                                                              @PathVariable Integer workflowVersionNo) {
        WorkflowDefinitionDTO definition = workflowApplicationService.findDefinition(workflowCode, workflowVersionNo)
                .orElseThrow(() -> new IllegalStateException("未找到工作流定义"));
        return SingleResult.of(workflowApplicationService.syncDefinition(definition));
    }

    @PostMapping("/{workflowCode}/{workflowVersionNo}/online")
    public SingleResult<WorkflowDefinitionDTO> onlineDefinition(@PathVariable String workflowCode,
                                                                @PathVariable Integer workflowVersionNo) {
        return SingleResult.of(workflowApplicationService.onlineDefinition(workflowCode, workflowVersionNo));
    }

    @PostMapping("/{workflowCode}/{workflowVersionNo}/offline")
    public SingleResult<WorkflowDefinitionDTO> offlineDefinition(@PathVariable String workflowCode,
                                                                 @PathVariable Integer workflowVersionNo) {
        return SingleResult.of(workflowApplicationService.offlineDefinition(workflowCode, workflowVersionNo));
    }

    @DeleteMapping("/{workflowCode}/{workflowVersionNo}")
    public SingleResult<Boolean> deleteDefinition(@PathVariable String workflowCode,
                                                  @PathVariable Integer workflowVersionNo) {
        return SingleResult.of(workflowApplicationService.deleteDefinition(workflowCode, workflowVersionNo));
    }

    @PostMapping("/{workflowCode}/{workflowVersionNo}/run")
    public SingleResult<WorkflowInstanceDTO> runDefinition(@PathVariable String workflowCode,
                                                           @PathVariable Integer workflowVersionNo) {
        return SingleResult.of(workflowApplicationService.runDefinition(workflowCode, workflowVersionNo));
    }

    @PostMapping("/validate")
    public SingleResult<WorkflowDefinitionDTO> validateDefinition(@Valid @RequestBody WorkflowDefinitionDTO request) {
        return SingleResult.of(workflowApplicationService.validateDefinition(request));
    }

    @GetMapping
    public MultiResult<WorkflowDefinitionDTO> listDefinitions() {
        return MultiResult.of(workflowApplicationService.listDefinitions());
    }

    @GetMapping("/platforms")
    public MultiResult<WorkflowPlatformMetadataDTO> listPlatforms() {
        return MultiResult.of(workflowApplicationService.listPlatforms());
    }

    @GetMapping("/{workflowCode}/{workflowVersionNo}")
    public SingleResult<WorkflowDefinitionDTO> getDefinition(@PathVariable String workflowCode,
                                                             @PathVariable Integer workflowVersionNo) {
        return SingleResult.of(workflowApplicationService.findDefinition(workflowCode, workflowVersionNo)
                .orElseThrow(() -> new IllegalStateException("未找到工作流定义")));
    }

    @GetMapping("/instances")
    public PageResult<WorkflowInstanceViewDTO> listInstances(@RequestParam(value = "workflowCode", required = false) String workflowCode,
                                                             @RequestParam(value = "workflowVersionNo", required = false) Integer workflowVersionNo,
                                                             @RequestParam(value = "platformType", required = false) String platformType,
                                                             @RequestParam(value = "workflowName", required = false) String workflowName,
                                                             @RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "businessKey", required = false) String businessKey,
                                                             @RequestParam(value = "instanceId", required = false) String instanceId,
                                                             @RequestParam(value = "externalInstanceId", required = false) String externalInstanceId,
                                                             @RequestParam(value = "stageCode", required = false) String stageCode,
                                                             @RequestParam(value = "triggerTimeFrom", required = false) String triggerTimeFrom,
                                                             @RequestParam(value = "triggerTimeTo", required = false) String triggerTimeTo,
                                                             @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                             @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        WorkflowInstanceQueryRequest request = WorkflowInstanceQueryRequest.builder()
                .workflowCode(workflowCode)
                .workflowVersionNo(workflowVersionNo)
                .platformType(parsePlatformType(platformType))
                .workflowName(workflowName)
                .status(status)
                .businessKey(businessKey)
                .instanceId(instanceId)
                .externalInstanceId(externalInstanceId)
                .stageCode(stageCode)
                .triggerTimeFrom(triggerTimeFrom)
                .triggerTimeTo(triggerTimeTo)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
        return workflowApplicationService.listInstances(request);
    }

    @PostMapping("/instances/trigger")
    public SingleResult<WorkflowInstanceDTO> trigger(@Valid @RequestBody WorkflowTriggerRequest request) {
        return SingleResult.of(workflowApplicationService.trigger(request));
    }

    @PostMapping("/instances/{instanceId}/stop")
    public SingleResult<WorkflowInstanceDTO> stop(@PathVariable String instanceId,
                                                  @RequestBody(required = false) WorkflowStopRequest request) {
        return SingleResult.of(workflowApplicationService.stop(instanceId, request));
    }

    @PostMapping("/instances/{instanceId}/pause")
    public SingleResult<WorkflowInstanceDTO> pause(@PathVariable String instanceId,
                                                   @RequestBody(required = false) WorkflowPauseRequest request) {
        return SingleResult.of(workflowApplicationService.pause(instanceId, request));
    }

    @PostMapping("/instances/{instanceId}/resume")
    public SingleResult<WorkflowInstanceDTO> resume(@PathVariable String instanceId,
                                                    @RequestBody(required = false) WorkflowResumeRequest request) {
        return SingleResult.of(workflowApplicationService.resume(instanceId, request));
    }

    @PostMapping("/instances/{instanceId}/retry")
    public SingleResult<WorkflowInstanceDTO> retry(@PathVariable String instanceId,
                                                   @RequestBody(required = false) WorkflowRetryRequest request) {
        return SingleResult.of(workflowApplicationService.retry(instanceId, request));
    }

    @GetMapping("/instances/{instanceId}")
    public SingleResult<WorkflowInstanceDTO> getInstance(@PathVariable String instanceId) {
        return SingleResult.of(workflowApplicationService.findInstance(instanceId)
                .orElseThrow(() -> new IllegalStateException("未找到任务实例")));
    }

    @GetMapping("/instances/{instanceId}/tasks")
    public SingleResult<WorkflowTaskListDTO> listTasks(@PathVariable String instanceId) {
        return SingleResult.of(workflowApplicationService.listTaskInstances(instanceId));
    }

    @GetMapping("/instances/{instanceId}/tasks/{taskInstanceId}/log")
    public SingleResult<String> getTaskLog(@PathVariable String instanceId,
                                           @PathVariable Long taskInstanceId) {
        return SingleResult.of(workflowApplicationService.queryTaskLog(instanceId, taskInstanceId));
    }

    @GetMapping("/task-instances")
    public SingleResult<WorkflowTaskInstancePageDTO> listTaskInstances(@RequestParam(value = "workflowCode", required = false) String workflowCode,
                                                                      @RequestParam(value = "workflowVersionNo", required = false) Integer workflowVersionNo,
                                                                      @RequestParam(value = "taskName", required = false) String taskName,
                                                                      @RequestParam(value = "workflowInstanceName", required = false) String workflowInstanceName,
                                                                      @RequestParam(value = "status", required = false) String status,
                                                                      @RequestParam(value = "startTimeFrom", required = false) String startTimeFrom,
                                                                      @RequestParam(value = "endTimeTo", required = false) String endTimeTo,
                                                                      @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        WorkflowTaskInstanceQueryRequest request = WorkflowTaskInstanceQueryRequest.builder()
                .workflowCode(workflowCode)
                .workflowVersionNo(workflowVersionNo)
                .taskName(taskName)
                .workflowInstanceName(workflowInstanceName)
                .status(status)
                .startTimeFrom(startTimeFrom)
                .endTimeTo(endTimeTo)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
        return SingleResult.of(workflowApplicationService.listTaskInstances(request));
    }

    @PostMapping("/task-instances/{taskInstanceId}/force-success")
    public SingleResult<Boolean> forceTaskSuccess(@PathVariable Long taskInstanceId,
                                                  @RequestParam(value = "workflowCode") String workflowCode,
                                                  @RequestParam(value = "workflowVersionNo") Integer workflowVersionNo) {
        workflowApplicationService.forceTaskSuccess(workflowCode, workflowVersionNo, taskInstanceId);
        return SingleResult.of(Boolean.TRUE);
    }

    @GetMapping("/task-instances/{taskInstanceId}/log")
    public SingleResult<String> getTaskLog(@PathVariable Long taskInstanceId,
                                           @RequestParam(value = "workflowCode") String workflowCode,
                                           @RequestParam(value = "workflowVersionNo") Integer workflowVersionNo,
                                           @RequestParam(value = "skipLineNum", required = false, defaultValue = "0") Integer skipLineNum,
                                           @RequestParam(value = "limit", required = false, defaultValue = "1000") Integer limit) {
        // 当前前端直接展示原始日志文本，分页参数保留用于与 DolphinScheduler 对齐。
        return SingleResult.of(workflowApplicationService.queryTaskLog(
                workflowCode,
                workflowVersionNo,
                taskInstanceId,
                skipLineNum,
                limit));
    }

    @PostMapping("/instances/{instanceId}/callbacks")
    public SingleResult<WorkflowInstanceDTO> callback(@PathVariable String instanceId,
                                                      @RequestBody WorkflowCallbackRequest request) {
        return SingleResult.of(workflowApplicationService.callback(instanceId, request));
    }

    private com.yss.valset.workflow.model.EtlPlatformType parsePlatformType(String platformType) {
        if (platformType == null || platformType.trim().isEmpty()) {
            return null;
        }
        try {
            return com.yss.valset.workflow.model.EtlPlatformType.valueOf(platformType.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }
}
