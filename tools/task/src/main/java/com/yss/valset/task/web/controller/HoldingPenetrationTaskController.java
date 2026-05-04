package com.yss.valset.task.web.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskBatchCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskActionResultDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDetailDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.service.HoldingPenetrationTaskService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 持仓穿透任务管理接口。
 */
@RestController
@RequestMapping("/holding-penetration-tasks")
public class HoldingPenetrationTaskController {

    private final HoldingPenetrationTaskService holdingPenetrationTaskService;

    public HoldingPenetrationTaskController(HoldingPenetrationTaskService holdingPenetrationTaskService) {
        this.holdingPenetrationTaskService = holdingPenetrationTaskService;
    }

    @GetMapping("/summary")
    @Operation(summary = "查询持仓穿透任务总览")
    public SingleResult<OutsourcedDataTaskSummaryDTO> summary(@RequestParam(value = "batchId", required = false) String batchId,
                                                              @RequestParam(value = "taskDate", required = false) String taskDate,
                                                              @RequestParam(value = "businessDate", required = false) String businessDate,
                                                              @RequestParam(value = "managerName", required = false) String managerName,
                                                              @RequestParam(value = "productKeyword", required = false) String productKeyword,
                                                              @RequestParam(value = "stage", required = false) String stage,
                                                              @RequestParam(value = "step", required = false) String step,
                                                              @RequestParam(value = "status", required = false) String status,
                                                              @RequestParam(value = "sourceType", required = false) String sourceType,
                                                              @RequestParam(value = "errorType", required = false) String errorType,
                                                              @RequestParam(value = "includeHistory", required = false) Boolean includeHistory) {
        return SingleResult.of(holdingPenetrationTaskService.summary(buildQuery(batchId, taskDate, businessDate, managerName, productKeyword, stage, step, status, sourceType, errorType, includeHistory, null, null)));
    }

    @GetMapping
    @Operation(summary = "分页查询持仓穿透任务")
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(@RequestParam(value = "batchId", required = false) String batchId,
                                                            @RequestParam(value = "taskDate", required = false) String taskDate,
                                                            @RequestParam(value = "businessDate", required = false) String businessDate,
                                                            @RequestParam(value = "managerName", required = false) String managerName,
                                                            @RequestParam(value = "productKeyword", required = false) String productKeyword,
                                                            @RequestParam(value = "stage", required = false) String stage,
                                                            @RequestParam(value = "step", required = false) String step,
                                                            @RequestParam(value = "status", required = false) String status,
                                                            @RequestParam(value = "sourceType", required = false) String sourceType,
                                                            @RequestParam(value = "errorType", required = false) String errorType,
                                                            @RequestParam(value = "includeHistory", required = false) Boolean includeHistory,
                                                            @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return holdingPenetrationTaskService.pageTasks(buildQuery(batchId, taskDate, businessDate, managerName, productKeyword, stage, step, status, sourceType, errorType, includeHistory, pageIndex, pageSize));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "查询持仓穿透任务详情")
    public SingleResult<OutsourcedDataTaskBatchDetailDTO> getTask(@PathVariable String batchId) {
        return SingleResult.of(holdingPenetrationTaskService.getTask(batchId));
    }

    @GetMapping("/{batchId}/steps")
    @Operation(summary = "查询持仓穿透任务步骤明细")
    public MultiResult<OutsourcedDataTaskStepDTO> listSteps(@PathVariable String batchId) {
        return MultiResult.of(holdingPenetrationTaskService.listSteps(batchId));
    }

    @GetMapping("/{batchId}/logs")
    @Operation(summary = "分页查询持仓穿透任务日志")
    public PageResult<OutsourcedDataTaskLogDTO> pageLogs(@PathVariable String batchId,
                                                         @RequestParam(value = "stage", required = false) String stage,
                                                         @RequestParam(value = "step", required = false) String step,
                                                         @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return holdingPenetrationTaskService.pageLogs(batchId, StringUtils.hasText(step) ? step : stage, pageIndex, pageSize);
    }

    @PostMapping("/{batchId}/execute")
    @Operation(summary = "手动执行持仓穿透任务")
    public SingleResult<OutsourcedDataTaskActionResultDTO> execute(@PathVariable String batchId,
                                                                   @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(holdingPenetrationTaskService.execute(batchId, command));
    }

    @PostMapping("/{batchId}/retry")
    @Operation(summary = "全流程重跑持仓穿透任务")
    public SingleResult<OutsourcedDataTaskActionResultDTO> retry(@PathVariable String batchId,
                                                                 @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(holdingPenetrationTaskService.retry(batchId, command));
    }

    @PostMapping("/{batchId}/stop")
    @Operation(summary = "停止持仓穿透任务")
    public SingleResult<OutsourcedDataTaskActionResultDTO> stop(@PathVariable String batchId,
                                                                @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(holdingPenetrationTaskService.stop(batchId, command));
    }

    @PostMapping("/{batchId}/steps/{stepId}/retry")
    @Operation(summary = "重跑持仓穿透任务步骤")
    public SingleResult<OutsourcedDataTaskActionResultDTO> retryStep(@PathVariable String batchId,
                                                                     @PathVariable String stepId,
                                                                     @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(holdingPenetrationTaskService.retryStep(batchId, stepId, command));
    }

    @PostMapping("/batch-execute")
    @Operation(summary = "批量手动执行持仓穿透任务")
    public MultiResult<OutsourcedDataTaskActionResultDTO> batchExecute(@Valid @RequestBody OutsourcedDataTaskBatchCommand command) {
        return MultiResult.of(holdingPenetrationTaskService.batchExecute(command));
    }

    @PostMapping("/batch-retry")
    @Operation(summary = "批量全流程重跑持仓穿透任务")
    public MultiResult<OutsourcedDataTaskActionResultDTO> batchRetry(@Valid @RequestBody OutsourcedDataTaskBatchCommand command) {
        return MultiResult.of(holdingPenetrationTaskService.batchRetry(command));
    }

    @PostMapping("/batch-stop")
    @Operation(summary = "批量停止持仓穿透任务")
    public MultiResult<OutsourcedDataTaskActionResultDTO> batchStop(@Valid @RequestBody OutsourcedDataTaskBatchCommand command) {
        return MultiResult.of(holdingPenetrationTaskService.batchStop(command));
    }

    private OutsourcedDataTaskQueryCommand buildQuery(String batchId,
                                                      String taskDate,
                                                      String businessDate,
                                                      String managerName,
                                                      String productKeyword,
                                                      String stage,
                                                      String step,
                                                      String status,
                                                      String sourceType,
                                                      String errorType,
                                                      Boolean includeHistory,
                                                      Integer pageIndex,
                                                      Integer pageSize) {
        OutsourcedDataTaskQueryCommand query = new OutsourcedDataTaskQueryCommand();
        query.setBatchId(batchId);
        query.setTaskDate(taskDate);
        query.setBusinessDate(businessDate);
        query.setManagerName(managerName);
        query.setProductKeyword(productKeyword);
        query.setStage(StringUtils.hasText(step) ? step : stage);
        query.setStatus(status);
        query.setSourceType(sourceType);
        query.setErrorType(errorType);
        query.setIncludeHistory(includeHistory);
        query.setPageIndex(pageIndex);
        query.setPageSize(pageSize);
        return query;
    }
}
