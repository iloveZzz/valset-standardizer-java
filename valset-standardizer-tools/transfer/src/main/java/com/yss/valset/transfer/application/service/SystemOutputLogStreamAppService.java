package com.yss.valset.transfer.application.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 系统输出日志流式推送服务。
 */
public interface SystemOutputLogStreamAppService {

    SseEmitter subscribe(String nodeId, Integer limit);
}
