package com.yss.valset.controller;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.TaskCreateResponse;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.service.WorkflowTaskAppService;
import com.yss.valset.application.service.WorkflowTaskQueryAppService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流任务 API 入口。
 */
@RestController
@RequestMapping("/tasks")
public class WorkflowTaskController {

    private final WorkflowTaskAppService taskAppService;
    private final WorkflowTaskQueryAppService taskQueryAppService;

    public WorkflowTaskController(WorkflowTaskAppService taskAppService, WorkflowTaskQueryAppService taskQueryAppService) {
        this.taskAppService = taskAppService;
        this.taskQueryAppService = taskQueryAppService;
    }

    /**
     * 创建解析任务。
     *
     * @param command 解析任务请求
     * @return 任务创建结果
     */
    @PostMapping("/parse")
    @Operation(summary = "创建估值表解析任务", description = "解析任务针对 Excel/CSV 输入时必须携带 fileId，用于定位对应的 ODS 原始行数据。")
    public SingleResult<TaskCreateResponse> createParseTask(@Valid @RequestBody ParseTaskCommand command) {
        return SingleResult.of(taskAppService.createParseTask(command));
    }

    /**
     * 创建匹配任务。
     *
     * @param command 匹配任务请求
     * @return 任务创建结果
     */
    @PostMapping("/match")
    @Operation(summary = "创建估值表匹配任务", description = "匹配任务针对 Excel/CSV 输入时必须携带 fileId，用于定位对应的 ODS 原始行数据。")
    public SingleResult<TaskCreateResponse> createMatchTask(@Valid @RequestBody MatchTaskCommand command) {
        return SingleResult.of(taskAppService.createMatchTask(command));
    }

    /**
     * 创建评估任务。
     *
     * @param command 评估任务请求
     * @return 任务创建结果
     */
    @PostMapping("/evaluate")
    @Operation(summary = "创建映射评估任务", description = "评估任务用于历史映射离线评估，不依赖 fileId。")
    public SingleResult<TaskCreateResponse> createEvaluateTask(@Valid @RequestBody EvaluateMappingTaskCommand command) {
        return SingleResult.of(taskAppService.createEvaluateTask(command));
    }

    /**
     * 创建原始数据提取任务。
     *
     * @param command 原始数据提取任务请求
     * @return 任务创建结果
     */
    @PostMapping("/extract")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建原始数据提取任务", description = "提取任务负责将 Excel/CSV 原始行数据落到 ODS 表，后续解析和匹配会通过 fileId 使用这些数据。")
    public SingleResult<TaskCreateResponse> createExtractTask(@Valid @RequestBody ExtractDataTaskCommand command) {
        return SingleResult.of(taskAppService.createExtractTask(command));
    }

    /**
     * 通过id查询任务。
     *
     * @param taskId 任务主键
     * @return 任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务详情")
    public SingleResult<TaskViewDTO> queryTask(@PathVariable Long taskId) {
        return SingleResult.of(taskQueryAppService.queryTask(taskId));
    }
}
