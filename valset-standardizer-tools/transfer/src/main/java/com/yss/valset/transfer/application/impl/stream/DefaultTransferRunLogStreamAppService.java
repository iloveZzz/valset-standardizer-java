package com.yss.valset.transfer.application.impl.stream;

import com.yss.valset.transfer.application.dto.TransferRunLogStreamMessageDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogViewDTO;
import com.yss.valset.transfer.application.service.TransferRunLogQueryService;
import com.yss.valset.transfer.application.service.TransferRunLogStreamAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
@Service
@RequiredArgsConstructor
public class DefaultTransferRunLogStreamAppService implements TransferRunLogStreamAppService {

    private static final int DEFAULT_LIMIT = 2000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransferRunLogQueryService transferRunLogQueryService;
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
            emitter.complete();
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
            cleanup.run();
            emitter.completeWithError(exception);
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
                cleanup.run();
                emitter.completeWithError(exception);
            } catch (RuntimeException exception) {
                cleanup.run();
                emitter.completeWithError(exception);
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
}
