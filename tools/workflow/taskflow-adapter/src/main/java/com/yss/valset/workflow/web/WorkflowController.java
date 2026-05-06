package com.yss.valset.workflow.web;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.spi.WorkflowApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/etl/workflows")
public class WorkflowController {

    private final WorkflowApplicationService workflowApplicationService;

    public WorkflowController(WorkflowApplicationService workflowApplicationService) {
        this.workflowApplicationService = workflowApplicationService;
    }

    @PostMapping
    public SingleResult<WorkflowDefinitionDTO> saveDefinition(@Valid @RequestBody WorkflowDefinitionDTO request) {
        return SingleResult.of(workflowApplicationService.saveDefinition(request));
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

    @PostMapping("/instances/trigger")
    public SingleResult<WorkflowInstanceDTO> trigger(@Valid @RequestBody WorkflowTriggerRequest request) {
        return SingleResult.of(workflowApplicationService.trigger(request));
    }

    @PostMapping("/instances/{instanceId}/stop")
    public SingleResult<WorkflowInstanceDTO> stop(@PathVariable String instanceId,
                                                  @RequestBody(required = false) WorkflowStopRequest request) {
        return SingleResult.of(workflowApplicationService.stop(instanceId, request));
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

    @GetMapping("/instances/{instanceId}/logs")
    public MultiResult<WorkflowStageLogDTO> listLogs(@PathVariable String instanceId,
                                                     @RequestParam(required = false) String stageCode) {
        return MultiResult.of(workflowApplicationService.listStageLogs(WorkflowLogQueryRequest.builder()
                .instanceId(instanceId)
                .stageCode(stageCode)
                .build()));
    }

    @PostMapping("/instances/{instanceId}/callbacks")
    public SingleResult<WorkflowInstanceDTO> callback(@PathVariable String instanceId,
                                                      @RequestBody WorkflowCallbackRequest request) {
        return SingleResult.of(workflowApplicationService.callback(instanceId, request));
    }
}
