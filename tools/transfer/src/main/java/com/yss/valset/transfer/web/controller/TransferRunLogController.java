package com.yss.valset.transfer.web.controller;

import com.yss.valset.transfer.application.command.TransferRunLogRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferRunLogRedeliverResponse;
import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.dto.TransferRunLogAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogViewDTO;
import com.yss.valset.transfer.application.service.TransferRunLogManagementAppService;
import com.yss.valset.transfer.application.service.TransferRunLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件收发运行日志查询接口。
 */
@RestController
@RequestMapping("/api/transfer-run-logs")
public class TransferRunLogController {

    private final TransferRunLogQueryService transferRunLogQueryService;
    private final TransferRunLogManagementAppService transferRunLogManagementAppService;

    public TransferRunLogController(TransferRunLogQueryService transferRunLogQueryService,
                                    TransferRunLogManagementAppService transferRunLogManagementAppService) {
        this.transferRunLogQueryService = transferRunLogQueryService;
        this.transferRunLogManagementAppService = transferRunLogManagementAppService;
    }

    /**
     * 查询文件收发运行日志列表。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param limit 查询上限
     * @return 文件收发运行日志列表
     */
    @GetMapping
    @Operation(summary = "查询文件收发运行日志列表", description = "支持按来源、文件、路由、阶段、状态和触发类型查询运行日志。")
    public MultiResult<TransferRunLogViewDTO> listLogs(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                       @RequestParam(value = "transferId", required = false) String transferId,
                                                       @RequestParam(value = "routeId", required = false) String routeId,
                                                       @RequestParam(value = "runStage", required = false) String runStage,
                                                       @RequestParam(value = "runStatus", required = false) String runStatus,
                                                       @RequestParam(value = "triggerType", required = false) String triggerType,
                                                       @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferRunLogQueryService.listLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, limit));
    }

    /**
     * 分页查询文件收发运行日志。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param keyword 关键字
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 文件收发运行日志分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询文件收发运行日志", description = "支持按来源、文件、路由、阶段、状态、触发类型和关键字分页查询运行日志，返回成功和错误信息。")
    public PageResult<TransferRunLogViewDTO> pageLogs(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                      @RequestParam(value = "transferId", required = false) String transferId,
                                                      @RequestParam(value = "routeId", required = false) String routeId,
                                                      @RequestParam(value = "runStage", required = false) String runStage,
                                                      @RequestParam(value = "runStatus", required = false) String runStatus,
                                                      @RequestParam(value = "triggerType", required = false) String triggerType,
                                                      @RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferRunLogQueryService.pageLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, keyword, pageIndex, pageSize);
    }

    /**
     * 统计分析文件收发运行日志。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param keyword 关键字
     * @return 文件收发运行日志统计分析结果
     */
    @GetMapping("/analysis")
    @Operation(summary = "统计分析文件收发运行日志", description = "按来源、文件、路由、阶段、状态、触发类型和关键字统计 run_stage 与 run_status 的数量分布。")
    public SingleResult<TransferRunLogAnalysisViewDTO> analyzeLogs(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                                   @RequestParam(value = "transferId", required = false) String transferId,
                                                                   @RequestParam(value = "routeId", required = false) String routeId,
                                                                   @RequestParam(value = "runStage", required = false) String runStage,
                                                                   @RequestParam(value = "runStatus", required = false) String runStatus,
                                                                   @RequestParam(value = "triggerType", required = false) String triggerType,
                                                                   @RequestParam(value = "keyword", required = false) String keyword) {
        return SingleResult.of(transferRunLogQueryService.analyzeLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, keyword));
    }

    /**
     * 批量重新投递失败的文件收发运行日志。
     *
     * @param command 重投递命令
     * @return 重投递结果
     */
    @PostMapping("/redeliver")
    @Operation(summary = "批量重新投递失败的文件收发运行日志", description = "仅支持对目标投递失败的运行日志执行批量重新投递，输入运行日志主键集合即可。")
    public SingleResult<TransferRunLogRedeliverResponse> redeliver(@RequestBody TransferRunLogRedeliverCommand command) {
        return SingleResult.of(transferRunLogManagementAppService.redeliver(command));
    }
}
