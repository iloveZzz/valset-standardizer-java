package com.yss.valset.analysis.application.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 解析生命周期事件流服务。
 */
public interface ParseLifecycleEventStreamAppService {

    SseEmitter subscribe(String source,
                         String queueId,
                         String transferId,
                         Long taskId,
                         String stage);
}
