package com.yss.subjectmatch.controller;

import com.yss.subjectmatch.application.command.EvaluateMappingTaskCommand;
import com.yss.subjectmatch.application.command.ExtractDataTaskCommand;
import com.yss.subjectmatch.application.command.MatchTaskCommand;
import com.yss.subjectmatch.application.command.ParseTaskCommand;
import com.yss.subjectmatch.application.dto.TaskCreateResponse;
import com.yss.subjectmatch.application.dto.TaskViewDTO;
import com.yss.subjectmatch.application.service.TaskAppService;
import com.yss.subjectmatch.application.service.TaskQueryAppService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 任务API入口点。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskAppService taskAppService;
    private final TaskQueryAppService taskQueryAppService;

    public TaskController(TaskAppService taskAppService, TaskQueryAppService taskQueryAppService) {
        this.taskAppService = taskAppService;
        this.taskQueryAppService = taskQueryAppService;
    }

    /**
     * 创建解析任务。
     */
    @PostMapping("/parse")
    @Operation(summary = "创建估值表解析任务", description = "解析任务针对 Excel/CSV 输入时必须携带 fileId，用于定位对应的 ODS 原始行数据。")
    public TaskCreateResponse createParseTask(@Valid @RequestBody ParseTaskCommand command) {
        return taskAppService.createParseTask(command);
    }

    /**
     * 创建匹配任务。
     */
    @PostMapping("/match")
    @Operation(summary = "创建估值表匹配任务", description = "匹配任务针对 Excel/CSV 输入时必须携带 fileId，用于定位对应的 ODS 原始行数据。")
    public TaskCreateResponse createMatchTask(@Valid @RequestBody MatchTaskCommand command) {
        return taskAppService.createMatchTask(command);
    }

    /**
     * 创建评估任务。
     */
    @PostMapping("/evaluate")
    @Operation(summary = "创建映射评估任务", description = "评估任务用于历史映射离线评估，不依赖 fileId。")
    public TaskCreateResponse createEvaluateTask(@Valid @RequestBody EvaluateMappingTaskCommand command) {
        return taskAppService.createEvaluateTask(command);
    }

    /**
     * 创建原始数据提取任务。
     */
    @PostMapping("/extract")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建原始数据提取任务", description = "提取任务负责将 Excel/CSV 原始行数据落到 ODS 表，后续解析和匹配会通过 fileId 使用这些数据。")
    public TaskCreateResponse createExtractTask(@Valid @RequestBody ExtractDataTaskCommand command) {
        return taskAppService.createExtractTask(command);
    }

    /**
     * 通过id查询任务。
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务详情")
    public TaskViewDTO queryTask(@PathVariable Long taskId) {
        return taskQueryAppService.queryTask(taskId);
    }
}
