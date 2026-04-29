package com.yss.valset.analysis.web.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.analysis.application.command.ParseQueueBackfillCommand;
import com.yss.valset.analysis.application.command.ParseQueueCompleteCommand;
import com.yss.valset.analysis.application.command.ParseQueueFailCommand;
import com.yss.valset.analysis.application.command.ParseQueueGenerateCommand;
import com.yss.valset.analysis.application.command.ParseQueueQueryCommand;
import com.yss.valset.analysis.application.command.ParseQueueRetryCommand;
import com.yss.valset.analysis.application.command.ParseQueueSubscribeCommand;
import com.yss.valset.analysis.application.dto.ParseQueueViewDTO;
import com.yss.valset.analysis.application.service.ParseQueueManagementAppService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 待解析任务接口。
 */
@RestController
@RequestMapping("/transfer-parse-queues")
public class ParseQueueController {

    private final ParseQueueManagementAppService transferParseQueueManagementAppService;

    public ParseQueueController(ParseQueueManagementAppService transferParseQueueManagementAppService) {
        this.transferParseQueueManagementAppService = transferParseQueueManagementAppService;
    }

    @GetMapping
    @Operation(summary = "查询待解析任务列表")
    public PageResult<ParseQueueViewDTO> pageQueues(@RequestParam(value = "transferId", required = false) String transferId,
                                                            @RequestParam(value = "businessKey", required = false) String businessKey,
                                                            @RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                            @RequestParam(value = "routeId", required = false) String routeId,
                                                            @RequestParam(value = "tagCode", required = false) String tagCode,
                                                            @RequestParam(value = "fileStatus", required = false) String fileStatus,
                                                            @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus,
                                                            @RequestParam(value = "parseStatus", required = false) String parseStatus,
                                                            @RequestParam(value = "triggerMode", required = false) String triggerMode,
                                                            @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferParseQueueManagementAppService.pageQueues(buildQuery(transferId, businessKey, sourceCode, routeId, tagCode, fileStatus, deliveryStatus, parseStatus, triggerMode, pageIndex, pageSize));
    }

    @GetMapping("/{queueId}")
    @Operation(summary = "查询待解析任务详情")
    public SingleResult<ParseQueueViewDTO> getQueue(@PathVariable String queueId) {
        return SingleResult.of(transferParseQueueManagementAppService.getQueue(queueId));
    }

    @PostMapping("/generate")
    @Operation(summary = "生成待解析任务")
    public SingleResult<ParseQueueViewDTO> generateQueue(@Valid @RequestBody ParseQueueGenerateCommand command) {
        return SingleResult.of(transferParseQueueManagementAppService.generateQueue(command));
    }

    @PostMapping("/backfill")
    @Operation(summary = "补漏生成待解析任务")
    public MultiResult<ParseQueueViewDTO> backfillQueues(@Valid @RequestBody ParseQueueBackfillCommand command) {
        List<ParseQueueViewDTO> data = transferParseQueueManagementAppService.backfillQueues(command);
        return MultiResult.of(data);
    }

    @PostMapping("/{queueId}/subscribe")
    @Operation(summary = "订阅待解析事件")
    public SingleResult<ParseQueueViewDTO> subscribeQueue(@PathVariable String queueId,
                                                             @Valid @RequestBody ParseQueueSubscribeCommand command) {
        return SingleResult.of(transferParseQueueManagementAppService.subscribeQueue(queueId, command));
    }

    @PostMapping("/{queueId}/complete")
    @Operation(summary = "完成待解析任务")
    public SingleResult<ParseQueueViewDTO> completeQueue(@PathVariable String queueId,
                                                                 @Valid @RequestBody ParseQueueCompleteCommand command) {
        return SingleResult.of(transferParseQueueManagementAppService.completeQueue(queueId, command));
    }

    @PostMapping("/{queueId}/fail")
    @Operation(summary = "标记待解析任务失败")
    public SingleResult<ParseQueueViewDTO> failQueue(@PathVariable String queueId,
                                                             @Valid @RequestBody ParseQueueFailCommand command) {
        return SingleResult.of(transferParseQueueManagementAppService.failQueue(queueId, command));
    }

    @PostMapping("/{queueId}/retry")
    @Operation(summary = "重试待解析任务")
    public SingleResult<ParseQueueViewDTO> retryQueue(@PathVariable String queueId,
                                                              @Valid @RequestBody ParseQueueRetryCommand command) {
        return SingleResult.of(transferParseQueueManagementAppService.retryQueue(queueId, command));
    }

    private ParseQueueQueryCommand buildQuery(String transferId,
                                                      String businessKey,
                                                      String sourceCode,
                                                      String routeId,
                                                      String tagCode,
                                                      String fileStatus,
                                                      String deliveryStatus,
                                                      String parseStatus,
                                                      String triggerMode,
                                                      Integer pageIndex,
                                                      Integer pageSize) {
        ParseQueueQueryCommand query = new ParseQueueQueryCommand();
        query.setTransferId(transferId);
        query.setBusinessKey(businessKey);
        query.setSourceCode(sourceCode);
        query.setRouteId(routeId);
        query.setTagCode(tagCode);
        query.setFileStatus(fileStatus);
        query.setDeliveryStatus(deliveryStatus);
        query.setParseStatus(parseStatus);
        query.setTriggerMode(triggerMode);
        query.setPageIndex(pageIndex);
        query.setPageSize(pageSize);
        return query;
    }
}
