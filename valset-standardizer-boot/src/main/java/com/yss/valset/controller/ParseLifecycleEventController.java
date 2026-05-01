package com.yss.valset.controller;

import com.yss.valset.analysis.application.service.ParseLifecycleEventStreamAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 解析生命周期事件流控制器。
 */
@RestController
@RequestMapping("/parse-lifecycle-events")
@RequiredArgsConstructor
public class ParseLifecycleEventController {

    private final ParseLifecycleEventStreamAppService parseLifecycleEventStreamAppService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam(required = false) String source,
                                @RequestParam(required = false) String queueId,
                                @RequestParam(required = false) String transferId,
                                @RequestParam(required = false) Long taskId,
                                @RequestParam(required = false) String stage) {
        return parseLifecycleEventStreamAppService.subscribe(source, queueId, transferId, taskId, stage);
    }
}
