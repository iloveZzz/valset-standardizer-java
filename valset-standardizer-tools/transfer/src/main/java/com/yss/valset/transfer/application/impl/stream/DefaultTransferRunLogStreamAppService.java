package com.yss.valset.transfer.application.impl.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.application.dto.TransferRunLogStreamMessageDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogViewDTO;
import com.yss.valset.transfer.application.service.TransferRunLogQueryService;
import com.yss.valset.transfer.application.service.TransferRunLogStreamAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认文件收发运行日志流式推送服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTransferRunLogStreamAppService implements TransferRunLogStreamAppService {

    private static final int DEFAULT_LIMIT = 2000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransferRunLogQueryService transferRunLogQueryService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1,
            runnable -> {
                Thread thread = new Thread(runnable, "transfer-run-log-stream");
                thread.setDaemon(true);
                return thread;
            }
    );

    @Override
    public SseEmitter subscribe(String sourceId,
                                String transferId,
                                String routeId,
                                String runStage,
                                String runStatus,
                                String triggerType,
                                Integer limit) {
        int maxSize = normalizeLimit(limit);
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        Set<String> deliveredKeys = ConcurrentHashMap.newKeySet();
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
            publishSnapshot(
                    emitter,
                    deliveredKeys,
                    sourceId,
                    transferId,
                    routeId,
                    runStage,
                    runStatus,
                    triggerType,
                    maxSize
            );
        } catch (IOException exception) {
            handleFailure(emitter, cleanup, "日志流初始推送失败", exception);
            return emitter;
        } catch (RuntimeException exception) {
            handleFailure(emitter, cleanup, "日志流初始处理异常", exception);
            return emitter;
        }

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            if (closed.get()) {
                return;
            }
            try {
                publishDelta(
                        emitter,
                        deliveredKeys,
                        sourceId,
                        transferId,
                        routeId,
                        runStage,
                        runStatus,
                        triggerType,
                        maxSize
                );
            } catch (IOException exception) {
                handleFailure(emitter, cleanup, "日志流推送失败", exception);
            } catch (RuntimeException exception) {
                handleFailure(emitter, cleanup, "日志流处理异常", exception);
            }
        }, 1L, 1L, TimeUnit.SECONDS);
        futureRef.set(future);
        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void publishSnapshot(SseEmitter emitter,
                                 Set<String> deliveredKeys,
                                 String sourceId,
                                 String transferId,
                                 String routeId,
                                 String runStage,
                                 String runStatus,
                                 String triggerType,
                                 int limit) throws IOException {
        List<TransferRunLogViewDTO> logs = loadLogs(
                sourceId,
                transferId,
                routeId,
                runStage,
                runStatus,
                triggerType,
                limit
        );
        publishLogs(emitter, deliveredKeys, logs);
    }

    private void publishDelta(SseEmitter emitter,
                              Set<String> deliveredKeys,
                              String sourceId,
                              String transferId,
                              String routeId,
                              String runStage,
                              String runStatus,
                              String triggerType,
                              int limit) throws IOException {
        List<TransferRunLogViewDTO> logs = loadLogs(
                sourceId,
                transferId,
                routeId,
                runStage,
                runStatus,
                triggerType,
                limit
        );
        publishLogs(emitter, deliveredKeys, logs);
        emitter.send(SseEmitter.event().comment("heartbeat"));
    }

    private void publishLogs(SseEmitter emitter,
                             Set<String> deliveredKeys,
                             List<TransferRunLogViewDTO> logs) throws IOException {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        List<TransferRunLogViewDTO> chronological = new ArrayList<>(logs);
        Collections.reverse(chronological);
        for (TransferRunLogViewDTO log : chronological) {
            if (log == null) {
                continue;
            }
            String key = resolveKey(log);
            if (!deliveredKeys.add(key)) {
                continue;
            }
            emitter.send(SseEmitter.event()
                    .data(new TransferRunLogStreamMessageDTO("log", "transfer-run-log-overview", log), MediaType.APPLICATION_JSON));
        }
    }

    private List<TransferRunLogViewDTO> loadLogs(String sourceId,
                                                 String transferId,
                                                 String routeId,
                                                 String runStage,
                                                 String runStatus,
                                                 String triggerType,
                                                 int limit) {
        return transferRunLogQueryService.listLogs(
                sourceId,
                transferId,
                routeId,
                runStage,
                runStatus,
                triggerType,
                null,
                limit
        );
    }

    private String resolveKey(TransferRunLogViewDTO log) {
        if (log == null) {
            return "__null__";
        }
        if (StringUtils.hasText(log.getRunLogId())) {
            return log.getRunLogId().trim();
        }
        String createdAt = log.getCreatedAt() == null ? "-" : TIME_FORMATTER.format(log.getCreatedAt());
        return createdAt + "::" + String.join("::",
                valueOrDash(log.getSourceId()),
                valueOrDash(log.getTransferId()),
                valueOrDash(log.getRouteId()),
                valueOrDash(log.getRunStage()),
                valueOrDash(log.getRunStatus()),
                valueOrDash(log.getLogMessage()),
                valueOrDash(log.getErrorMessage()));
    }

    private String valueOrDash(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
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
        log.warn("{}，准备结束日志流: {}", message, throwable.getMessage(), throwable);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(buildErrorPayload(message, throwable), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException sendException) {
            log.debug("日志流错误事件发送失败，直接结束连接: {}", sendException.getMessage());
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
