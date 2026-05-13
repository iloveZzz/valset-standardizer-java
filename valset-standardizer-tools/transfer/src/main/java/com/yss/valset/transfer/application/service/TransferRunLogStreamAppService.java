package com.yss.valset.transfer.application.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 文件收发运行日志流式推送服务。
 */
public interface TransferRunLogStreamAppService {

    /**
     * 订阅运行日志流。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param limit 初始拉取上限
     * @return SSE 连接
     */
    SseEmitter subscribe(String sourceId,
                         String transferId,
                         String routeId,
                         String runStage,
                         String runStatus,
                         String triggerType,
                         Integer limit);
}
