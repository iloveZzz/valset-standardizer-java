package com.yss.valset.transfer.application.impl.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 默认文件收取进度推送服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferIngestProgressAppService implements TransferIngestProgressAppService {

    private final ObjectMapper objectMapper;
    private final Map<String, Set<SseEmitter>> emitterRegistry = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("来源主键不能为空");
        }
        SseEmitter emitter = new SseEmitter();
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

    private void send(String sourceId, String type, Object data) {
        if (sourceId == null || sourceId.isBlank()) {
            return;
        }
        Set<SseEmitter> emitters = emitterRegistry.get(sourceId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new TransferSseMessage<>(type, sourceId, data));
        } catch (Exception exception) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException exception) {
                removeEmitter(sourceId, emitter);
            } catch (IllegalStateException exception) {
                removeEmitter(sourceId, emitter);
            } catch (RuntimeException exception) {
                removeEmitter(sourceId, emitter);
            }
        }
    }

    private void removeEmitter(String sourceId, SseEmitter emitter) {
        if (sourceId == null || sourceId.isBlank()) {
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
        if (status == null || status.isBlank()) {
            return "idle";
        }
        return status.trim().toLowerCase();
    }

    private record TransferSseMessage<T>(String type, String taskId, T data) {
    }

    private record ProgressData(long processedCount,
                                long totalCount,
                                String message) {
    }

    private record MessageData(String message) {
    }

    private record StatusData(String status, String message, String triggerType, String triggeredAt) {
    }

    private record CompleteData(String message) {
    }

    private record ErrorData(String code, String message) {
    }
}
