package com.yss.valset.transfer.application.impl.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.application.dto.SystemOutputLogStreamMessageDTO;
import com.yss.valset.transfer.application.dto.SystemOutputLogViewDTO;
import com.yss.valset.transfer.application.service.SystemOutputLogStreamAppService;
import com.yss.valset.transfer.infrastructure.logging.InMemorySystemOutputLogStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认系统输出日志流式推送服务。
 */
@Slf4j
@Service
public class DefaultSystemOutputLogStreamAppService implements SystemOutputLogStreamAppService {

    private static final int DEFAULT_LIMIT = InMemorySystemOutputLogStore.DEFAULT_MAX_LINES;

    private final ObjectMapper objectMapper;

    private final InMemorySystemOutputLogStore logStore = InMemorySystemOutputLogStore.getInstance();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1,
            runnable -> {
                Thread thread = new Thread(runnable, "system-output-log-stream");
                thread.setDaemon(true);
                return thread;
            }
    );

    public DefaultSystemOutputLogStreamAppService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter subscribe(String nodeId, Integer limit) {
        int maxSize = normalizeLimit(limit);
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicLong latestSequence = new AtomicLong(0L);
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        Runnable cleanup = () -> {
            if (closed.compareAndSet(false, true)) {
                ScheduledFuture<?> future = futureRef.getAndSet(null);
                if (future != null) {
                    future.cancel(true);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            completeSilently(emitter);
        });
        emitter.onError(throwable -> cleanup.run());

        try {
            publishLogs(emitter, latestSequence, logStore.latest(nodeId, maxSize));
        } catch (IOException exception) {
            handleFailure(emitter, cleanup, "系统输出日志初始推送失败", exception);
            return emitter;
        } catch (RuntimeException exception) {
            handleFailure(emitter, cleanup, "系统输出日志初始处理异常", exception);
            return emitter;
        }

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            if (closed.get()) {
                return;
            }
            try {
                publishLogs(emitter, latestSequence, logStore.after(nodeId, latestSequence.get(), maxSize));
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException exception) {
                handleFailure(emitter, cleanup, "系统输出日志推送失败", exception);
            } catch (RuntimeException exception) {
                handleFailure(emitter, cleanup, "系统输出日志处理异常", exception);
            }
        }, 1L, 1L, TimeUnit.SECONDS);
        futureRef.set(future);
        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void publishLogs(SseEmitter emitter,
                             AtomicLong latestSequence,
                             List<SystemOutputLogViewDTO> logs) throws IOException {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        for (SystemOutputLogViewDTO log : logs) {
            if (log == null || log.getSequence() <= latestSequence.get()) {
                continue;
            }
            latestSequence.set(log.getSequence());
            emitter.send(SseEmitter.event()
                    .data(new SystemOutputLogStreamMessageDTO("log", "system-output-log", log), MediaType.APPLICATION_JSON));
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, DEFAULT_LIMIT);
    }

    private void handleFailure(SseEmitter emitter, Runnable cleanup, String message, Throwable throwable) {
        cleanup.run();
        if (isClientDisconnect(throwable)) {
            log.debug("{}，检测到客户端断开连接: {}", message, throwable.getMessage());
            completeSilently(emitter);
            return;
        }
        log.warn("{}，准备结束系统输出日志流: {}", message, throwable.getMessage(), throwable);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(buildErrorPayload(message, throwable), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException sendException) {
            log.debug("系统输出日志错误事件发送失败，直接结束连接: {}", sendException.getMessage());
        }
        completeSilently(emitter);
    }

    private String buildErrorPayload(String message, Throwable throwable) {
        Map<String, String> payload = new HashMap<>(2);
        payload.put("message", message);
        payload.put("detail", throwable == null ? null : throwable.getMessage());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "{\"message\":\"" + escapeJson(message) + "\",\"detail\":\"" + escapeJson(
                    throwable == null ? null : throwable.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void completeSilently(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignore) {
            // 连接已关闭，忽略
        }
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ClosedChannelException) {
                return true;
            }
            String className = current.getClass().getName();
            if (className.contains("ClientAbortException")
                    || className.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase(Locale.ROOT);
                if (lowerMessage.contains("broken pipe")
                        || lowerMessage.contains("connection reset by peer")
                        || lowerMessage.contains("stream closed")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
