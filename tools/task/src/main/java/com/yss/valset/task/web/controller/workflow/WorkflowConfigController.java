package com.yss.valset.task.web.controller.workflow;

import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigSaveCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.service.workflow.WorkflowConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流配置管理接口。
 */
@RestController
@RequestMapping("/workflow-configs")
public class WorkflowConfigController {

    private final WorkflowConfigService workflowConfigService;

    public WorkflowConfigController(WorkflowConfigService workflowConfigService) {
        this.workflowConfigService = workflowConfigService;
    }

    @GetMapping
    @Operation(summary = "分页查询工作流配置")
    public PageResult<WorkflowDefinitionDTO> pageDefinitions(@RequestParam(value = "workflowCode", required = false) String workflowCode,
                                                             @RequestParam(value = "workflowName", required = false) String workflowName,
                                                             @RequestParam(value = "businessType", required = false) String businessType,
                                                             @RequestParam(value = "engineType", required = false) String engineType,
                                                             @RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                             @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                             @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        WorkflowConfigQueryCommand query = new WorkflowConfigQueryCommand();
        query.setWorkflowCode(workflowCode);
        query.setWorkflowName(workflowName);
        query.setBusinessType(businessType);
        query.setEngineType(engineType);
        query.setStatus(status);
        query.setEnabled(enabled);
        query.setPageIndex(pageIndex);
        query.setPageSize(pageSize);
        return workflowConfigService.pageDefinitions(query);
    }

    @GetMapping("/{workflowId}")
    @Operation(summary = "查询工作流配置详情")
    public SingleResult<WorkflowDefinitionDTO> getDefinition(@PathVariable String workflowId) {
        return SingleResult.of(workflowConfigService.getDefinition(workflowId));
    }

    @GetMapping("/active/{workflowCode}")
    @Operation(summary = "查询启用中的工作流配置")
    public SingleResult<WorkflowDefinitionDTO> getActiveDefinition(@PathVariable String workflowCode) {
        return SingleResult.of(workflowConfigService.getActiveDefinition(workflowCode));
    }

    @PostMapping("/draft")
    @Operation(summary = "保存工作流配置草稿")
    public SingleResult<WorkflowDefinitionDTO> saveDraft(@Valid @RequestBody WorkflowConfigSaveCommand command) {
        return SingleResult.of(workflowConfigService.saveDraft(command));
    }

    @PostMapping("/{workflowId}/publish")
    @Operation(summary = "发布工作流配置")
    public SingleResult<WorkflowDefinitionDTO> publish(@PathVariable String workflowId) {
        return SingleResult.of(workflowConfigService.publish(workflowId));
    }

    @PostMapping("/{workflowId}/disable")
    @Operation(summary = "停用工作流配置")
    public SingleResult<WorkflowDefinitionDTO> disable(@PathVariable String workflowId) {
        return SingleResult.of(workflowConfigService.disable(workflowId));
    }
}
