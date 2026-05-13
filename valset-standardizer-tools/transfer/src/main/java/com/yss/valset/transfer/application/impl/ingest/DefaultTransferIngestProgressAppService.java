package com.yss.valset.transfer.application.impl.ingest;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认文件收取进度推送服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferIngestProgressAppService implements TransferIngestProgressAppService {

    private static final long HEARTBEAT_INITIAL_DELAY_SECONDS = 15L;
    private static final long HEARTBEAT_PERIOD_SECONDS = 15L;

    private final Map<String, Set<SseEmitter>> emitterRegistry = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "transfer-ingest-progress-heartbeat");
                thread.setDaemon(true);
                return thread;
            }
    );

    @PostConstruct
    public void startHeartbeat() {
        heartbeatScheduler.scheduleWithFixedDelay(
                this::sendHeartbeatSafely,
                HEARTBEAT_INITIAL_DELAY_SECONDS,
                HEARTBEAT_PERIOD_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void shutdownHeartbeat() {
        heartbeatScheduler.shutdownNow();
    }

    @Override
    public SseEmitter subscribe(String sourceId) {
        if (sourceId == null || sourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("来源主键不能为空");
        }
        // 使用无限超时保持 SSE 订阅长连接，避免浏览器在默认超时后反复重连。
        SseEmitter emitter = new SseEmitter(0L);
        emitterRegistry.computeIfAbsent(sourceId, key -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(sourceId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(sourceId, emitter);
            emitter.complete();
        });
        emitter.onError(throwable -> removeEmitter(sourceId, emitter));
        return emitter;
    }

    @Override
    public void publishStatus(String sourceId, String status, String message, String triggerType, String triggeredAt) {
        send(sourceId, "status", new StatusData(normalizeStatus(status), message, triggerType, triggeredAt));
    }

    @Override
    public void publishProgress(String sourceId, long processedCount, long totalCount, String message) {
        send(sourceId, "progress", new ProgressData(processedCount, totalCount, message));
    }

    @Override
    public void publishMessage(String sourceId, String message) {
        send(sourceId, "message", new MessageData(message));
    }

    @Override
    public void publishComplete(String sourceId, String message) {
        send(sourceId, "complete", new CompleteData(message));
    }

    @Override
    public void publishError(String sourceId, String code, String message) {
        send(sourceId, "error", new ErrorData(code, message));
    }

    private void sendHeartbeatSafely() {
        emitterRegistry.forEach((sourceId, emitters) -> {
            if (emitters == null || emitters.isEmpty()) {
                return;
            }
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception exception) {
                    handleSendFailure(sourceId, emitter, exception);
                }
            }
        });
    }

    private void send(String sourceId, String type, Object data) {
        if (sourceId == null || sourceId.trim().isEmpty()) {
            return;
        }
        Set<SseEmitter> emitters = emitterRegistry.get(sourceId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(new TransferSseMessage<>(type, sourceId, data), MediaType.APPLICATION_JSON));
            } catch (Exception exception) {
                handleSendFailure(sourceId, emitter, exception);
            }
        }
    }

    private void handleSendFailure(String sourceId, SseEmitter emitter, Exception exception) {
        removeEmitter(sourceId, emitter);
        if (isClientDisconnect(exception)) {
            completeSilently(emitter);
        }
    }

    private void removeEmitter(String sourceId, SseEmitter emitter) {
        if (sourceId == null || sourceId.trim().isEmpty()) {
            return;
        }
        Set<SseEmitter> emitters = emitterRegistry.get(sourceId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emitterRegistry.remove(sourceId, emitters);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "idle";
        }
        return status.trim().toLowerCase();
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

    private void completeSilently(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignore) {
            // 连接已关闭，忽略
        }
    }

    private static final class TransferSseMessage<T> {
        private final String type;
        private final String taskId;
        private final T data;

        private TransferSseMessage(String type, String taskId, T data) {
            this.type = type;
            this.taskId = taskId;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public String getTaskId() {
            return taskId;
        }

        public T getData() {
            return data;
        }
    }

    private static final class ProgressData {
        private final long processedCount;
        private final long totalCount;
        private final String message;

        private ProgressData(long processedCount, long totalCount, String message) {
            this.processedCount = processedCount;
            this.totalCount = totalCount;
            this.message = message;
        }

        public long getProcessedCount() {
            return processedCount;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class MessageData {
        private final String message;

        private MessageData(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class StatusData {
        private final String status;
        private final String message;
        private final String triggerType;
        private final String triggeredAt;

        private StatusData(String status, String message, String triggerType, String triggeredAt) {
            this.status = status;
            this.message = message;
            this.triggerType = triggerType;
            this.triggeredAt = triggeredAt;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public String getTriggeredAt() {
            return triggeredAt;
        }
    }

    private static final class CompleteData {
        private final String message;

        private CompleteData(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class ErrorData {
        private final String code;
        private final String message;

        private ErrorData(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
