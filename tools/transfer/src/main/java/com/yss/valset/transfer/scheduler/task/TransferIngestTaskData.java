package com.yss.valset.transfer.scheduler.task;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.domain.model.SourceType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件收取任务入参。
 */
public record TransferIngestTaskData(
        String sourceId,
        String sourceType,
        String sourceCode,
        String triggerType,
        Map<String, Object> parameters,
        String ingestLockToken
) {

    public IngestTransferSourceCommand toCommand() {
        return new IngestTransferSourceCommand(
                sourceId,
                resolveSourceType(sourceType),
                sourceCode,
                triggerType,
                parameters == null ? Map.of() : new LinkedHashMap<>(parameters),
                ingestLockToken
        );
    }

    private SourceType resolveSourceType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        return SourceType.valueOf(rawType.trim().toUpperCase());
    }
}
