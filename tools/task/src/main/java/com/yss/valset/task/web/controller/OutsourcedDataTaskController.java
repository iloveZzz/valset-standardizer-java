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
import com.yss.valset.task.application.service.OutsourcedDataTaskManagementAppService;
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
 * 估值表解析任务管理接口。
 */
@RestController
@RequestMapping("/outsourced-data-tasks")
public class OutsourcedDataTaskController {

    private final OutsourcedDataTaskManagementAppService outsourcedDataTaskManagementAppService;

    public OutsourcedDataTaskController(OutsourcedDataTaskManagementAppService outsourcedDataTaskManagementAppService) {
        this.outsourcedDataTaskManagementAppService = outsourcedDataTaskManagementAppService;
    }

    @GetMapping("/summary")
    @Operation(summary = "查询估值表解析任务总览")
    public SingleResult<OutsourcedDataTaskSummaryDTO> summary(@RequestParam(value = "businessDate", required = false) String businessDate,
                                                              @RequestParam(value = "managerName", required = false) String managerName,
                                                              @RequestParam(value = "productKeyword", required = false) String productKeyword,
                                                              @RequestParam(value = "stage", required = false) String stage,
                                                              @RequestParam(value = "step", required = false) String step,
                                                              @RequestParam(value = "status", required = false) String status,
                                                              @RequestParam(value = "sourceType", required = false) String sourceType,
                                                              @RequestParam(value = "errorType", required = false) String errorType,
                                                              @RequestParam(value = "includeHistory", required = false) Boolean includeHistory) {
        return SingleResult.of(outsourcedDataTaskManagementAppService.summary(buildQuery(businessDate, managerName, productKeyword, stage, step, status, sourceType, errorType, includeHistory, null, null)));
    }

    @GetMapping
    @Operation(summary = "分页查询估值表解析任务")
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(@RequestParam(value = "businessDate", required = false) String businessDate,
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
        return outsourcedDataTaskManagementAppService.pageTasks(buildQuery(businessDate, managerName, productKeyword, stage, step, status, sourceType, errorType, includeHistory, pageIndex, pageSize));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "查询估值表解析任务详情")
    public SingleResult<OutsourcedDataTaskBatchDetailDTO> getTask(@PathVariable String batchId) {
        return SingleResult.of(outsourcedDataTaskManagementAppService.getTask(batchId));
    }

    @GetMapping("/{batchId}/steps")
    @Operation(summary = "查询估值表解析任务步骤明细")
    public MultiResult<OutsourcedDataTaskStepDTO> listSteps(@PathVariable String batchId) {
        return MultiResult.of(outsourcedDataTaskManagementAppService.listSteps(batchId));
    }

    @GetMapping("/{batchId}/logs")
    @Operation(summary = "分页查询估值表解析任务日志")
    public PageResult<OutsourcedDataTaskLogDTO> pageLogs(@PathVariable String batchId,
                                                         @RequestParam(value = "stage", required = false) String stage,
                                                         @RequestParam(value = "step", required = false) String step,
                                                         @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return outsourcedDataTaskManagementAppService.pageLogs(batchId, StringUtils.hasText(step) ? step : stage, pageIndex, pageSize);
    }

    @PostMapping("/{batchId}/execute")
    @Operation(summary = "执行估值表解析任务")
    public SingleResult<OutsourcedDataTaskActionResultDTO> execute(@PathVariable String batchId,
                                                                   @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(outsourcedDataTaskManagementAppService.execute(batchId, command));
    }

    @PostMapping("/{batchId}/retry")
    @Operation(summary = "重跑估值表解析任务")
    public SingleResult<OutsourcedDataTaskActionResultDTO> retry(@PathVariable String batchId,
                                                                 @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(outsourcedDataTaskManagementAppService.retry(batchId, command));
    }

    @PostMapping("/{batchId}/stop")
    @Operation(summary = "停止估值表解析任务")
    public SingleResult<OutsourcedDataTaskActionResultDTO> stop(@PathVariable String batchId,
                                                                @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(outsourcedDataTaskManagementAppService.stop(batchId, command));
    }

    @PostMapping("/{batchId}/steps/{stepId}/retry")
    @Operation(summary = "重跑估值表解析任务步骤")
    public SingleResult<OutsourcedDataTaskActionResultDTO> retryStep(@PathVariable String batchId,
                                                                     @PathVariable String stepId,
                                                                     @RequestBody(required = false) OutsourcedDataTaskActionCommand command) {
        return SingleResult.of(outsourcedDataTaskManagementAppService.retryStep(batchId, stepId, command));
    }

    @PostMapping("/batch-execute")
    @Operation(summary = "批量执行估值表解析任务")
    public MultiResult<OutsourcedDataTaskActionResultDTO> batchExecute(@Valid @RequestBody OutsourcedDataTaskBatchCommand command) {
        return MultiResult.of(outsourcedDataTaskManagementAppService.batchExecute(command));
    }

    @PostMapping("/batch-retry")
    @Operation(summary = "批量重跑估值表解析任务")
    public MultiResult<OutsourcedDataTaskActionResultDTO> batchRetry(@Valid @RequestBody OutsourcedDataTaskBatchCommand command) {
        return MultiResult.of(outsourcedDataTaskManagementAppService.batchRetry(command));
    }

    @PostMapping("/batch-stop")
    @Operation(summary = "批量停止估值表解析任务")
    public MultiResult<OutsourcedDataTaskActionResultDTO> batchStop(@Valid @RequestBody OutsourcedDataTaskBatchCommand command) {
        return MultiResult.of(outsourcedDataTaskManagementAppService.batchStop(command));
    }

    private OutsourcedDataTaskQueryCommand buildQuery(String businessDate,
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
