package com.yss.valset.transfer.application.command;

import com.yss.valset.transfer.domain.model.SourceType;

import java.util.Map;

/**
 * 收取文件命令。
 */
public record IngestTransferSourceCommand(
        String sourceId,
        SourceType sourceType,
        String sourceCode,
        String triggerType,
        Map<String, Object> parameters,
        String ingestLockToken
) {
}
