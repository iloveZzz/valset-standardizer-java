package com.yss.valset.batch.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.analysis.application.command.ParseQueueCompleteCommand;
import com.yss.valset.analysis.application.command.ParseQueueFailCommand;
import com.yss.valset.analysis.application.command.ParseQueueSubscribeCommand;
import com.yss.valset.analysis.application.service.ParseQueueManagementAppService;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEventPublisher;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.analysis.domain.gateway.ParseQueueGateway;
import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.batch.dispatcher.TaskDispatcher;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.application.service.ValsetFileInfoRepairAppService;
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
import java.util.LinkedHashMap;
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
    private final ValsetFileInfoRepairAppService valsetFileInfoRepairAppService;
    private final WorkflowTaskGateway taskGateway;
    private final TaskDispatcher taskDispatcher;
    private final ObjectMapper objectMapper;
    private final ParseLifecycleEventPublisher parseLifecycleEventPublisher;

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
        publishLifecycleEvent(ParseLifecycleStage.CYCLE_STARTED, null, null, "待解析观察者开始执行");
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
                publishLifecycleEvent(ParseLifecycleStage.BATCH_STARTED, null, null, "开始处理待解析批次", Map.of("batchSize", effectiveBatchSize));
                List<ParseQueue> pendingQueues = loadPendingQueues(effectiveBatchSize);
                if (pendingQueues.isEmpty()) {
                    publishLifecycleEvent(ParseLifecycleStage.BATCH_EMPTY, null, null, "本轮没有待处理事件", Map.of(
                            "success", totalSuccess,
                            "failed", totalFailed,
                            "skipped", totalSkipped
                    ));
                    publishLifecycleEvent(ParseLifecycleStage.CYCLE_FINISHED, null, null, "待解析观察者执行结束", Map.of(
                            "success", totalSuccess,
                            "failed", totalFailed,
                            "skipped", totalSkipped
                    ));
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
                publishLifecycleEvent(ParseLifecycleStage.BATCH_FINISHED, null, null, "待解析批次处理完成", Map.of(
                        "batchSize", pendingQueues.size(),
                        "batchSuccess", batchSuccess,
                        "batchFailed", batchFailed,
                        "batchSkipped", batchSkipped,
                        "totalSuccess", totalSuccess,
                        "totalFailed", totalFailed,
                        "totalSkipped", totalSkipped
                ));
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
        ParseTaskCommand parseTaskCommand = null;
        try {
            log.info("开始观察并处理待解析事件，queueId={}, businessKey={}", queueId, queue.businessKey());
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_DISCOVERED, queue, null, "观察到待解析事件");
            try {
                parseQueueManagementAppService.subscribeQueue(queueId, buildSubscribeCommand());
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                    log.info("待解析事件已被其他观察者接管，queueId={}", queueId);
                    publishLifecycleEvent(ParseLifecycleStage.QUEUE_SKIPPED, queue, null, "待解析事件已被其他观察者接管");
                    return ProcessOutcome.SKIPPED;
                }
                throw exception;
            }
            subscribed = true;
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_SUBSCRIBED, queue, null, "待解析事件接管成功");
            parseTaskCommand = buildParseTaskCommand(queue);
            publishLifecycleEvent(ParseLifecycleStage.TASK_REQUEST_BUILT, queue, null, "已构建解析任务命令", buildParseTaskAttributes(parseTaskCommand));
            Long taskId = createAndDispatchParseTask(parseTaskCommand, queue);
            if (taskId == null) {
                return ProcessOutcome.FAILED;
            }
            WorkflowTask workflowTask = taskGateway.findById(taskId);
            if (workflowTask == null || workflowTask.getTaskStatus() != TaskStatus.SUCCESS) {
                publishLifecycleEvent(ParseLifecycleStage.TASK_FAILED, queue, taskId, "解析任务未成功完成", buildParseTaskAttributes(parseTaskCommand));
                failQueue(queueId, "解析任务未成功完成，taskId=" + taskId);
                return ProcessOutcome.FAILED;
            }
            parseQueueManagementAppService.completeQueue(queueId, buildCompleteCommand(workflowTask.getResultPayload()));
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_COMPLETED, queue, taskId, "待解析事件处理完成", buildParseTaskAttributes(parseTaskCommand));
            log.info("待解析事件处理完成，queueId={}, taskId={}", queueId, taskId);
            return ProcessOutcome.SUCCESS;
        } catch (Exception exception) {
            log.error("待解析事件处理失败，queueId={}", queueId, exception);
            if (subscribed) {
                failQueue(queueId, TaskFailureClassifier.resolveReadableMessage(exception));
            }
            Map<String, Object> failedAttributes = new LinkedHashMap<>(buildParseTaskAttributes(parseTaskCommand));
            failedAttributes.put("errorMessage", exception.getMessage());
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_FAILED, queue, null, "待解析事件处理失败", failedAttributes);
            return ProcessOutcome.FAILED;
        }
    }

    private Long createAndDispatchParseTask(ParseTaskCommand parseTaskCommand, ParseQueue queue) {
        if (parseTaskCommand == null) {
            throw new IllegalStateException("解析任务命令不能为空，queueId=" + queue.queueId());
        }
        String businessKey = buildParseBusinessKey(parseTaskCommand);
        boolean forceRebuild = Boolean.TRUE.equals(parseTaskCommand.getForceRebuild());
        WorkflowTask reusableTask = forceRebuild ? null : taskGateway.findLatestSuccessfulTask(TaskType.PARSE_WORKBOOK, businessKey);
        if (reusableTask != null && reusableTask.getTaskId() != null) {
            log.info("复用已有的成功解析任务，queueId={}, taskId={}, businessKey={}", queue.queueId(), reusableTask.getTaskId(), businessKey);
            publishLifecycleEvent(ParseLifecycleStage.TASK_REUSED, queue, reusableTask.getTaskId(), "复用已有成功解析任务", buildParseTaskAttributes(parseTaskCommand));
            return reusableTask.getTaskId();
        }
        WorkflowTask workflowTask = WorkflowTask.builder()
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(TaskStatus.PENDING)
                .taskStage(TaskStage.PARSE)
                .businessKey(businessKey)
                .fileId(parseTaskCommand.getFileId())
                .inputPayload(writeValueAsString(parseTaskCommand))
                .build();
        Long taskId = taskGateway.save(workflowTask);
        publishLifecycleEvent(ParseLifecycleStage.TASK_CREATED, queue, taskId, "已创建解析任务", buildParseTaskAttributes(parseTaskCommand));
        taskDispatcher.dispatchTask(taskId);
        publishLifecycleEvent(ParseLifecycleStage.TASK_DISPATCHED, queue, taskId, "已派发解析任务", buildParseTaskAttributes(parseTaskCommand));
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
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_STARTED, queue, null, "文件主数据缺失，开始自动修复");
            fileInfo = valsetFileInfoRepairAppService.ensureFromTransferObject(transferObject);
            Map<String, Object> attributes = new LinkedHashMap<>();
            if (fileInfo != null && fileInfo.getFileId() != null) {
                attributes.put("fileId", fileInfo.getFileId());
            }
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_COMPLETED, queue, null, "文件主数据自动修复完成", attributes);
        }
        if (fileInfo == null || fileInfo.getFileId() == null) {
            publishLifecycleEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_FAILED, queue, null, "文件主数据自动修复失败");
            throw new IllegalStateException("未找到由 TransferObject 回写的文件主数据，且无法自动修复，无法继续订阅解析，transferId=" + queue.transferId());
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
        if (fileInfo != null && StringUtils.hasText(fileInfo.getRealStoragePath())) {
            return fileInfo.getRealStoragePath().trim();
        }
        if (fileInfo != null && StringUtils.hasText(fileInfo.getLocalTempPath())) {
            return fileInfo.getLocalTempPath().trim();
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

    private Map<String, Object> buildParseTaskAttributes(ParseTaskCommand command) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (command == null) {
            return attributes;
        }
        if (command.getFileId() != null) {
            attributes.put("fileId", command.getFileId());
        }
        if (StringUtils.hasText(command.getDataSourceType())) {
            attributes.put("dataSourceType", command.getDataSourceType());
        }
        if (StringUtils.hasText(command.getWorkbookPath())) {
            attributes.put("workbookPath", command.getWorkbookPath());
        }
        if (StringUtils.hasText(command.getFileNameOriginal())) {
            attributes.put("fileNameOriginal", command.getFileNameOriginal());
        }
        if (command.getForceRebuild() != null) {
            attributes.put("forceRebuild", command.getForceRebuild());
        }
        return attributes;
    }

    private void publishLifecycleEvent(ParseLifecycleStage stage, ParseQueue queue, String message) {
        publishLifecycleEvent(stage, queue, null, message, Map.of());
    }

    private void publishLifecycleEvent(ParseLifecycleStage stage, ParseQueue queue, Long taskId, String message) {
        publishLifecycleEvent(stage, queue, taskId, message, Map.of());
    }

    private void publishLifecycleEvent(ParseLifecycleStage stage, ParseQueue queue, Long taskId, String message, Map<String, Object> attributes) {
        if (parseLifecycleEventPublisher == null || stage == null) {
            return;
        }
        ParseLifecycleEvent.ParseLifecycleEventBuilder builder = ParseLifecycleEvent.builder()
                .stage(stage)
                .source("parse-queue-observer")
                .message(message);
        if (queue != null) {
            builder.queueId(queue.queueId())
                    .transferId(queue.transferId())
                    .businessKey(queue.businessKey())
                    .triggerMode(queue.triggerMode() == null ? null : queue.triggerMode().name())
                    .subscribedBy(queue.subscribedBy());
        }
        if (taskId != null) {
            builder.taskId(taskId);
        }
        if (attributes != null && !attributes.isEmpty()) {
            builder.attributes(new LinkedHashMap<>(attributes));
        }
        parseLifecycleEventPublisher.publish(builder.build());
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
