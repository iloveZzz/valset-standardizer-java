package com.yss.valset.batch.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.analysis.application.command.ParseQueueCompleteCommand;
import com.yss.valset.analysis.application.command.ParseQueueFailCommand;
import com.yss.valset.analysis.application.command.ParseQueueSubscribeCommand;
import com.yss.valset.analysis.application.service.ParseQueueManagementAppService;
import com.yss.valset.analysis.domain.gateway.ParseQueueGateway;
import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.batch.dispatcher.TaskDispatcher;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * 待解析事件观察者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseQueueObserverJob {

    private static final String DEFAULT_SUBSCRIBER = "解析观察者";

    private enum ProcessOutcome {
        SUCCESS,
        FAILED,
        SKIPPED
    }

    private final ParseQueueGateway parseQueueGateway;
    private final ParseQueueManagementAppService parseQueueManagementAppService;
    private final TransferObjectGateway transferObjectGateway;
    private final ValsetFileInfoGateway valsetFileInfoGateway;
    private final TaskGateway taskGateway;
    private final TaskDispatcher taskDispatcher;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${subject.match.parse-queue-observer.enabled:true}")
    private boolean enabled;

    @Value("${subject.match.parse-queue-observer.batch-size:10}")
    private int batchSize;

    @Value("${subject.match.parse-queue-observer.subscriber-name:解析观察者}")
    private String subscriberName;

    @Value("${subject.match.parse-queue-observer.startup-jitter-ms:0}")
    private long startupJitterMs;

    @Value("${subject.match.parse-queue-observer.batch-pause-ms:0}")
    private long batchPauseMs;

    /**
     * 轮询订阅并同步执行待解析任务。
     */
    @Scheduled(fixedDelayString = "${subject.match.parse-queue-observer.fixed-delay-ms:3000}")
    public void observePendingQueues() {
        sleepRandomStartupJitter();
        runObservation();
    }

    /**
     * 立即执行一轮待解析事件观察。
     */
    public ParseQueueObserverRunSummary runObservation() {
        if (!enabled) {
            return new ParseQueueObserverRunSummary(0, 0, 0, 0, 0, 0, 0);
        }
        if (!running.compareAndSet(false, true)) {
            return new ParseQueueObserverRunSummary(0, 0, 0, 0, 0, 0, 0);
        }
        long totalSuccess = 0;
        long totalFailed = 0;
        long totalSkipped = 0;
        try {
            int effectiveBatchSize = Math.max(1, batchSize);
            while (true) {
                List<ParseQueue> pendingQueues = loadPendingQueues(effectiveBatchSize);
                if (pendingQueues.isEmpty()) {
                    log.info("待解析观察者本轮没有待处理事件，批次处理结束，success={}, failed={}, skipped={}", totalSuccess, totalFailed, totalSkipped);
                    return new ParseQueueObserverRunSummary(effectiveBatchSize, totalSuccess, totalFailed, totalSkipped, totalSuccess, totalFailed, totalSkipped);
                }
                long batchSuccess = 0;
                long batchFailed = 0;
                long batchSkipped = 0;
                for (ParseQueue queue : pendingQueues) {
                    ProcessOutcome outcome = processQueue(queue);
                    if (outcome == ProcessOutcome.SUCCESS) {
                        batchSuccess++;
                        totalSuccess++;
                    } else if (outcome == ProcessOutcome.SKIPPED) {
                        batchSkipped++;
                        totalSkipped++;
                    } else {
                        batchFailed++;
                        totalFailed++;
                    }
                }
                log.info("待解析观察者批次处理完成，batchSize={}, success={}, failed={}, skipped={}, totalSuccess={}, totalFailed={}, totalSkipped={}",
                        pendingQueues.size(), batchSuccess, batchFailed, batchSkipped, totalSuccess, totalFailed, totalSkipped);
                sleepBatchPause();
            }
        } finally {
            running.set(false);
        }
    }

    private List<ParseQueue> loadPendingQueues(int batchSize) {
        return new ArrayList<>(parseQueueGateway.listPendingQueues(batchSize));
    }

    private void sleepRandomStartupJitter() {
        long effectiveJitter = Math.max(0L, startupJitterMs);
        if (effectiveJitter <= 0) {
            return;
        }
        long sleepMs = ThreadLocalRandom.current().nextLong(effectiveJitter + 1L);
        sleepQuietly(sleepMs, "启动抖动");
    }

    private void sleepBatchPause() {
        long effectivePause = Math.max(0L, batchPauseMs);
        if (effectivePause <= 0) {
            return;
        }
        sleepQuietly(effectivePause, "批次暂停");
    }

    private void sleepQuietly(long sleepMs, String reason) {
        if (sleepMs <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(sleepMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("待解析观察者{}被中断，sleepMs={}", reason, sleepMs);
        }
    }

    private ProcessOutcome processQueue(ParseQueue queue) {
        if (queue == null || !StringUtils.hasText(queue.queueId())) {
            return ProcessOutcome.FAILED;
        }
        String queueId = queue.queueId();
        boolean subscribed = false;
        try {
            log.info("开始观察并处理待解析事件，queueId={}, businessKey={}", queueId, queue.businessKey());
            try {
                parseQueueManagementAppService.subscribeQueue(queueId, buildSubscribeCommand());
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                    log.info("待解析事件已被其他观察者接管，queueId={}", queueId);
                    return ProcessOutcome.SKIPPED;
                }
                throw exception;
            }
            subscribed = true;
            ParseTaskCommand parseTaskCommand = buildParseTaskCommand(queue);
            Long taskId = createAndDispatchParseTask(parseTaskCommand, queue);
            if (taskId == null) {
                return ProcessOutcome.FAILED;
            }
            TaskInfo taskInfo = taskGateway.findById(taskId);
            if (taskInfo == null || taskInfo.getTaskStatus() != TaskStatus.SUCCESS) {
                failQueue(queueId, "解析任务未成功完成，taskId=" + taskId);
                return ProcessOutcome.FAILED;
            }
            parseQueueManagementAppService.completeQueue(queueId, buildCompleteCommand(taskInfo.getResultPayload()));
            log.info("待解析事件处理完成，queueId={}, taskId={}", queueId, taskId);
            return ProcessOutcome.SUCCESS;
        } catch (Exception exception) {
            log.error("待解析事件处理失败，queueId={}", queueId, exception);
            if (subscribed) {
                failQueue(queueId, TaskFailureClassifier.resolveReadableMessage(exception));
            }
            return ProcessOutcome.FAILED;
        }
    }

    private Long createAndDispatchParseTask(ParseTaskCommand parseTaskCommand, ParseQueue queue) {
        if (parseTaskCommand == null) {
            throw new IllegalStateException("解析任务命令不能为空，queueId=" + queue.queueId());
        }
        String businessKey = buildParseBusinessKey(parseTaskCommand);
        boolean forceRebuild = Boolean.TRUE.equals(parseTaskCommand.getForceRebuild());
        TaskInfo reusableTask = forceRebuild ? null : taskGateway.findLatestSuccessfulTask(TaskType.PARSE_WORKBOOK, businessKey);
        if (reusableTask != null && reusableTask.getTaskId() != null) {
            log.info("复用已有的成功解析任务，queueId={}, taskId={}, businessKey={}", queue.queueId(), reusableTask.getTaskId(), businessKey);
            return reusableTask.getTaskId();
        }
        TaskInfo taskInfo = TaskInfo.builder()
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(TaskStatus.PENDING)
                .taskStage(TaskStage.PARSE)
                .businessKey(businessKey)
                .fileId(parseTaskCommand.getFileId())
                .inputPayload(writeValueAsString(parseTaskCommand))
                .build();
        Long taskId = taskGateway.save(taskInfo);
        taskDispatcher.dispatchTask(taskId);
        return taskId;
    }

    private ParseQueueSubscribeCommand buildSubscribeCommand() {
        ParseQueueSubscribeCommand command = new ParseQueueSubscribeCommand();
        command.setSubscribedBy(resolveSubscriberName());
        return command;
    }

    private ParseTaskCommand buildParseTaskCommand(ParseQueue queue) {
        TransferObject transferObject = transferObjectGateway.findById(queue.transferId())
                .orElseThrow(() -> new IllegalStateException("未找到待解析事件对应的文件主对象，transferId=" + queue.transferId()));
        if (!StringUtils.hasText(transferObject.fingerprint())) {
            throw new IllegalStateException("文件主对象缺少指纹，无法定位文件主数据，transferId=" + queue.transferId());
        }
        ValsetFileInfo fileInfo = valsetFileInfoGateway.findByFingerprint(transferObject.fingerprint());
        if (fileInfo == null || fileInfo.getFileId() == null) {
            throw new IllegalStateException("未找到待解析事件对应的文件主数据，transferId=" + queue.transferId());
        }
        ParseTaskCommand command = new ParseTaskCommand();
        command.setDataSourceType(resolveDataSourceType(fileInfo, transferObject));
        command.setWorkbookPath(resolveWorkbookPath(fileInfo, transferObject));
        command.setFileId(fileInfo.getFileId());
        command.setFileNameOriginal(resolveFileNameOriginal(fileInfo, transferObject));
        command.setCreatedBy(resolveSubscriberName());
        command.setForceRebuild(resolveForceRebuild(queue));
        return command;
    }

    private ParseQueueFailCommand buildFailCommand(String message) {
        ParseQueueFailCommand command = new ParseQueueFailCommand();
        command.setErrorMessage(StringUtils.hasText(message) ? message : "结构化解析失败");
        return command;
    }

    private ParseQueueCompleteCommand buildCompleteCommand(String resultPayload) {
        ParseQueueCompleteCommand command = new ParseQueueCompleteCommand();
        if (!StringUtils.hasText(resultPayload)) {
            command.setParseResultJson(null);
            return command;
        }
        try {
            command.setParseResultJson(objectMapper.readValue(resultPayload, Object.class));
        } catch (Exception ignore) {
            command.setParseResultJson(resultPayload);
        }
        return command;
    }

    private void failQueue(String queueId, String errorMessage) {
        try {
            parseQueueManagementAppService.failQueue(queueId, buildFailCommand(errorMessage));
        } catch (Exception failException) {
            log.error("回写待解析事件失败状态失败，queueId={}", queueId, failException);
        }
    }

    private String resolveSubscriberName() {
        if (StringUtils.hasText(subscriberName)) {
            return subscriberName.trim();
        }
        return DEFAULT_SUBSCRIBER;
    }

    private String resolveDataSourceType(ValsetFileInfo fileInfo, TransferObject transferObject) {
        String candidate = fileInfo == null ? null : fileInfo.getFileFormat();
        if (!StringUtils.hasText(candidate) && transferObject != null) {
            candidate = transferObject.sourceType();
        }
        if (!StringUtils.hasText(candidate)) {
            return "EXCEL";
        }
        return candidate.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveWorkbookPath(ValsetFileInfo fileInfo, TransferObject transferObject) {
        if (fileInfo != null && StringUtils.hasText(fileInfo.getStorageUri())) {
            return fileInfo.getStorageUri().trim();
        }
        if (transferObject != null && StringUtils.hasText(transferObject.realStoragePath())) {
            return transferObject.realStoragePath().trim();
        }
        if (transferObject != null && StringUtils.hasText(transferObject.localTempPath())) {
            return transferObject.localTempPath().trim();
        }
        throw new IllegalStateException("无法定位解析工作簿路径，transferId=" + (transferObject == null ? null : transferObject.transferId()));
    }

    private String resolveFileNameOriginal(ValsetFileInfo fileInfo, TransferObject transferObject) {
        if (fileInfo != null && StringUtils.hasText(fileInfo.getFileNameOriginal())) {
            return fileInfo.getFileNameOriginal().trim();
        }
        if (transferObject != null && StringUtils.hasText(transferObject.originalName())) {
            return transferObject.originalName().trim();
        }
        return null;
    }

    private boolean resolveForceRebuild(ParseQueue queue) {
        if (queue == null || !StringUtils.hasText(queue.parseRequestJson())) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> request = objectMapper.readValue(queue.parseRequestJson(), Map.class);
            Object forceRebuild = request == null ? null : request.get("forceRebuild");
            if (forceRebuild instanceof Boolean value) {
                return value;
            }
            if (forceRebuild instanceof String text) {
                return Boolean.parseBoolean(text);
            }
        } catch (Exception exception) {
            log.warn("解析待解析事件重建标记失败，queueId={}", queue.queueId(), exception);
        }
        return false;
    }

    private String buildParseBusinessKey(ParseTaskCommand command) {
        return String.join(":",
                "WORKFLOW",
                "PARSE",
                normalizeDataSourceType(command == null ? null : command.getDataSourceType()),
                command == null || command.getFileId() == null ? "NO_FILE_ID" : String.valueOf(command.getFileId()));
    }

    private String normalizeDataSourceType(String dataSourceType) {
        if (!StringUtils.hasText(dataSourceType)) {
            return "EXCEL";
        }
        return dataSourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("序列化解析任务参数失败", exception);
        }
    }
}
