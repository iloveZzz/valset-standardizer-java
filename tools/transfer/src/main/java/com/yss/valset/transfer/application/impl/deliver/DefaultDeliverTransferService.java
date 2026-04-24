package com.yss.valset.transfer.application.impl.deliver;

import com.yss.valset.transfer.application.port.DeliverTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.domain.model.TransferRunStatus;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.model.config.TransferRouteConfig;
import com.yss.valset.transfer.infrastructure.plugin.TransferActionPluginRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认文件投递应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDeliverTransferService implements DeliverTransferUseCase {

    private final TransferRouteGateway transferRouteGateway;
    private final TransferObjectGateway transferObjectGateway;
    private final TransferTargetGateway transferTargetGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TransferActionPluginRegistry transferActionPluginRegistry;
    private final TransferRunLogGateway transferRunLogGateway;
    private final ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider;

    @Override
    public void execute(String routeId, String transferId) {
        TransferRoute route = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到路由记录，routeId=" + routeId));
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + transferId));
        TransferTarget target = transferTargetGateway.findByTargetCode(route.targetCode())
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetCode=" + route.targetCode()));
        String triggerType = resolveTriggerType(route.routeMeta());
        boolean failureLogged = false;
        try {
            int attemptIndex = (int) transferDeliveryGateway.countByRouteId(routeId);
            log.info("开始文件投递，routeId={}，transferId={}，targetCode={}，targetType={}，originalName={}，retryCount={}",
                    route.routeId(),
                    transferObject.transferId(),
                    target.targetCode(),
                    target.targetType(),
                    transferObject.originalName(),
                    attemptIndex);
            saveRunLog(
                    transferObject,
                    route,
                    triggerType,
                    TransferRunStage.DELIVER.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "开始投递，routeId=" + routeId
                            + "，transferId=" + transferId
                            + "，targetCode=" + target.targetCode()
                            + "，targetType=" + target.targetType(),
                    null
            );
            TransferContext context = new TransferContext(transferObject, route, target, buildAttributes(route, target));
            log.info("投递上下文已准备完成，routeId={}，transferId={}，targetCode={}，attemptIndex={}",
                    routeId,
                    transferId,
                    target.targetCode(),
                    attemptIndex);
            TransferResult result = transferActionPluginRegistry.getRequired(route).execute(context);
            transferDeliveryGateway.recordResult(routeId, transferId, result, attemptIndex);
            if (!result.success()) {
                log.warn("文件投递未成功，routeId={}，transferId={}，targetCode={}，attemptIndex={}，messages={}",
                        routeId,
                        transferId,
                        target.targetCode(),
                        attemptIndex,
                        result.messages());
                scheduleRetryIfNeeded(routeId, transferId, route, attemptIndex + 1);
                saveRunLog(
                        transferObject,
                        route,
                        triggerType,
                        TransferRunStage.DELIVER.name(),
                        TransferRunStatus.FAILED.name(),
                        "文件投递失败，routeId=" + routeId
                                + "，transferId=" + transferId
                                + "，targetCode=" + target.targetCode()
                                + "，messages=" + result.messages(),
                        null
                );
                failureLogged = true;
                throw new IllegalStateException("文件投递失败，routeId=" + routeId + ", messages=" + result.messages());
            }
            clearFailedDeliverRunLogs(transferObject.transferId());
            log.info("文件投递成功，routeId={}，transferId={}，targetCode={}，attemptIndex={}，messages={}",
                    routeId,
                    transferId,
                    target.targetCode(),
                    attemptIndex,
                    result.messages());
            saveRunLog(
                    transferObject,
                    route,
                    triggerType,
                    TransferRunStage.DELIVER.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "文件投递完成，routeId=" + routeId
                            + "，transferId=" + transferId
                            + "，targetCode=" + route.targetCode()
                            + "，targetType=" + target.targetType(),
                    null
            );
        } catch (RuntimeException exception) {
            if (!failureLogged) {
                saveRunLog(
                        transferObject,
                        route,
                        triggerType,
                        TransferRunStage.DELIVER.name(),
                        TransferRunStatus.FAILED.name(),
                        "文件投递异常，routeId=" + routeId
                                + "，transferId=" + transferId
                                + "，targetCode=" + route.targetCode(),
                        exception
                );
            }
            throw exception;
        }
    }

    private void clearFailedDeliverRunLogs(String transferId) {
        try {
            long deletedCount = transferRunLogGateway.deleteFailedDeliverLogsByTransferId(transferId);
            log.info("已清理投递失败运行日志，transferId={}，deletedCount={}", transferId, deletedCount);
        } catch (RuntimeException exception) {
            log.warn("清理投递失败运行日志失败，transferId={}，error={}", transferId, exception.getMessage(), exception);
        }
    }

    private void scheduleRetryIfNeeded(String routeId, String transferId, TransferRoute route, int nextAttempt) {
        TransferRouteConfig routeConfig = TransferRouteConfig.from(route);
        int maxRetryCount = routeConfig.maxRetryCount();
        int retryDelaySeconds = routeConfig.retryDelaySeconds();
        if (nextAttempt < maxRetryCount) {
            log.info("准备调度投递重试，routeId={}，transferId={}，nextAttempt={}，maxRetryCount={}，delaySeconds={}",
                    routeId,
                    transferId,
                    nextAttempt,
                    maxRetryCount,
                    retryDelaySeconds);
            transferJobSchedulerProvider.getObject().scheduleDeliverRetry(routeId, transferId, nextAttempt, retryDelaySeconds);
        } else {
            log.info("投递重试次数已达到上限，routeId={}，transferId={}，nextAttempt={}，maxRetryCount={}",
                    routeId,
                    transferId,
                    nextAttempt,
                    maxRetryCount);
        }
    }

    private Map<String, Object> buildAttributes(TransferRoute route, TransferTarget target) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        TransferRouteConfig routeConfig = TransferRouteConfig.from(route);
        attributes.putAll(routeConfig.toMetaMap());
        if (route != null && route.routeMeta() != null) {
            route.routeMeta().forEach(attributes::putIfAbsent);
        }
        attributes.put(TransferConfigKeys.TARGET_ID, target.targetId());
        attributes.put(TransferConfigKeys.TARGET_CODE, target.targetCode());
        attributes.put(TransferConfigKeys.TARGET_NAME, target.targetName());
        attributes.put(TransferConfigKeys.TARGET_TYPE, target.targetType() == null ? null : target.targetType().name());
        attributes.put(TransferConfigKeys.TARGET_PATH_TEMPLATE, target.targetPathTemplate());
        if (target.connectionConfig() != null) {
            attributes.putAll(target.connectionConfig());
        }
        if (target.targetMeta() != null) {
            attributes.putAll(target.targetMeta());
        }
        return attributes;
    }

    private String resolveTriggerType(Map<String, Object> routeMeta) {
        Object raw = routeMeta == null ? null : routeMeta.get(TransferConfigKeys.TRIGGER_TYPE);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        return String.valueOf(raw).trim().toUpperCase();
    }

    private void saveRunLog(TransferObject transferObject,
                            TransferRoute route,
                            String triggerType,
                            String runStage,
                            String runStatus,
                            String logMessage,
                            Throwable error) {
        if (error == null) {
            log.info("文件投递运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}",
                    runStage,
                    runStatus,
                    transferObject == null ? null : transferObject.sourceId(),
                    transferObject == null ? null : transferObject.transferId(),
                    route == null ? null : route.routeId(),
                    logMessage);
        } else {
            log.error("文件投递运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}，error={}",
                    runStage,
                    runStatus,
                    transferObject == null ? null : transferObject.sourceId(),
                    transferObject == null ? null : transferObject.transferId(),
                    route == null ? null : route.routeId(),
                    logMessage,
                    buildErrorMessage(error),
                    error);
        }
        transferRunLogGateway.save(new TransferRunLog(
                null,
                transferObject.sourceId(),
                transferObject.sourceType(),
                transferObject.sourceCode(),
                null,
                transferObject.transferId(),
                route == null ? null : route.routeId(),
                triggerType,
                runStage,
                runStatus,
                logMessage,
                error == null ? null : buildErrorMessage(error),
                LocalDateTime.now()
        ));
    }

    private String buildErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 3) {
            if (depth > 0) {
                builder.append(" -> ");
            }
            builder.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                builder.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return builder.toString();
    }
}
