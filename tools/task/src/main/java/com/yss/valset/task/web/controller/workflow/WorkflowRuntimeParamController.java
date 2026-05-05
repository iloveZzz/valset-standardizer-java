package com.yss.valset.task.web.controller.workflow;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;
import com.yss.valset.application.service.workflow.WorkflowRuntimeParamService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流运行参数接口。
 */
@RestController
@RequestMapping("/workflow-configs/runtime-params")
public class WorkflowRuntimeParamController {

    private final WorkflowRuntimeParamService workflowRuntimeParamService;

    public WorkflowRuntimeParamController(WorkflowRuntimeParamService workflowRuntimeParamService) {
        this.workflowRuntimeParamService = workflowRuntimeParamService;
    }

    @GetMapping
    @Operation(summary = "查询工作流运行参数")
    public SingleResult<WorkflowRuntimeParamDTO> getRuntimeParam() {
        return SingleResult.of(workflowRuntimeParamService.getRuntimeParam());
    }

    @PutMapping
    @Operation(summary = "保存工作流运行参数")
    public SingleResult<WorkflowRuntimeParamDTO> saveRuntimeParam(@Valid @RequestBody WorkflowRuntimeParamSaveCommand command) {
        return SingleResult.of(workflowRuntimeParamService.saveRuntimeParam(command));
    }
}
