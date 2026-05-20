package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.transfer.application.dto.SystemOutputLogCleanupResponse;
import com.yss.valset.transfer.application.dto.SystemOutputLogNodeDTO;
import com.yss.valset.transfer.application.dto.SystemOutputLogViewDTO;
import com.yss.valset.transfer.application.service.SystemOutputLogStreamAppService;
import com.yss.valset.transfer.infrastructure.logging.InMemorySystemOutputLogStore;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 系统输出日志查询接口。
 */
@RestController
@RequestMapping("/system-output-logs")
public class SystemOutputLogController {

    private final SystemOutputLogStreamAppService systemOutputLogStreamAppService;

    private final InMemorySystemOutputLogStore logStore = InMemorySystemOutputLogStore.getInstance();

    public SystemOutputLogController(SystemOutputLogStreamAppService systemOutputLogStreamAppService) {
        this.systemOutputLogStreamAppService = systemOutputLogStreamAppService;
    }

    /**
     * 查询最近的系统输出日志。
     *
     * @param nodeId 节点标识
     * @param limit 查询上限，默认最近 10000 行
     * @return 系统输出日志行
     */
    @GetMapping
    @Operation(summary = "查询系统输出日志", description = "从内存环形缓冲区查询最近的系统输出日志，可按节点标识过滤，默认保留最近 10000 行。")
    public MultiResult<SystemOutputLogViewDTO> listLogs(@RequestParam(value = "nodeId", required = false) String nodeId,
                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(logStore.latest(nodeId, limit));
    }

    /**
     * 查询可查看的系统输出日志节点。
     *
     * @return 系统输出日志节点
     */
    @GetMapping("/nodes")
    @Operation(summary = "查询系统输出日志节点", description = "返回当前可查看的系统输出日志节点，用于区分微服务实例。")
    public MultiResult<SystemOutputLogNodeDTO> listNodes() {
        return MultiResult.of(logStore.nodes());
    }

    /**
     * 清空当前内存中的系统输出日志。
     *
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    @Operation(summary = "清理系统输出日志", description = "清空当前内存中的系统输出日志，可按节点标识过滤，后续日志会继续写入内存环形缓冲区。")
    public SingleResult<SystemOutputLogCleanupResponse> cleanupLogs(@RequestParam(value = "nodeId", required = false) String nodeId) {
        return SingleResult.of(logStore.clear(nodeId));
    }

    /**
     * 订阅系统输出日志流。
     *
     * @param nodeId 节点标识
     * @param limit 初始日志数量上限，默认最近 10000 行
     * @return SSE 连接
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅系统输出日志流", description = "通过 SSE 持续推送系统输出日志，可按节点标识过滤，内存中默认仅保留最近 10000 行。")
    public SseEmitter streamLogs(@RequestParam(value = "nodeId", required = false) String nodeId,
                                 @RequestParam(value = "limit", required = false) Integer limit) {
        return systemOutputLogStreamAppService.subscribe(nodeId, limit);
    }
}
