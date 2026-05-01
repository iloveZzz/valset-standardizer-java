package com.yss.valset.analysis.application.impl.stream;

import com.yss.valset.analysis.application.service.ParseLifecycleEventStreamAppService;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认解析生命周期事件流服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultParseLifecycleEventStreamAppService implements ParseLifecycleEventStreamAppService {

    private final Set<Subscriber> subscribers = ConcurrentHashMap.newKeySet();

    @Override
    public SseEmitter subscribe(String source,
                                String queueId,
                                String transferId,
                                Long taskId,
                                String stage) {
        SseEmitter emitter = new SseEmitter(0L);
        Subscriber subscriber = new Subscriber(
                emitter,
                normalize(source),
                normalize(queueId),
                normalize(transferId),
                taskId,
                normalize(stage)
        );
        subscribers.add(subscriber);
        emitter.onCompletion(() -> remove(subscriber));
        emitter.onTimeout(() -> {
            remove(subscriber);
            emitter.complete();
        });
        emitter.onError(throwable -> remove(subscriber));
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("已订阅解析生命周期事件流", MediaType.TEXT_PLAIN));
        } catch (IOException exception) {
            remove(subscriber);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @EventListener
    public void onParseLifecycleEvent(ParseLifecycleEvent event) {
        if (event == null) {
            return;
        }
        List<Subscriber> snapshot = new ArrayList<>(subscribers);
        for (Subscriber subscriber : snapshot) {
            if (!subscriber.matches(event)) {
                continue;
            }
            try {
                subscriber.emitter.send(SseEmitter.event()
                        .name("parse-lifecycle")
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException exception) {
                remove(subscriber);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        for (Subscriber subscriber : new ArrayList<>(subscribers)) {
            remove(subscriber);
            try {
                subscriber.emitter.complete();
            } catch (Exception ignore) {
                // 忽略关闭异常
            }
        }
    }

    private void remove(Subscriber subscriber) {
        if (subscriber != null) {
            subscribers.remove(subscriber);
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record Subscriber(SseEmitter emitter,
                              String source,
                              String queueId,
                              String transferId,
                              Long taskId,
                              String stage) {

        private boolean matches(ParseLifecycleEvent event) {
            if (event == null) {
                return false;
            }
            if (StringUtils.hasText(source) && !source.equalsIgnoreCase(normalize(event.getSource()))) {
                return false;
            }
            if (StringUtils.hasText(queueId) && !queueId.equalsIgnoreCase(normalize(event.getQueueId()))) {
                return false;
            }
            if (StringUtils.hasText(transferId) && !transferId.equalsIgnoreCase(normalize(event.getTransferId()))) {
                return false;
            }
            if (taskId != null && !taskId.equals(event.getTaskId())) {
                return false;
            }
            if (StringUtils.hasText(stage)) {
                String eventStage = event.getStage() == null ? null : event.getStage().name();
                if (eventStage == null || !stage.equalsIgnoreCase(eventStage)) {
                    return false;
                }
            }
            return true;
        }

        private String normalize(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            return value.trim();
        }
    }
}
