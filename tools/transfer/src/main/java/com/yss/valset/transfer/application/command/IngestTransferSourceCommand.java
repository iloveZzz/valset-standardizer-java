package com.yss.valset.transfer.application.command;

import com.yss.valset.transfer.domain.model.SourceType;

import java.util.Map;

/**
 * 收取文件命令。
 */
public record IngestTransferSourceCommand(
        Long sourceId,
        SourceType sourceType,
        String sourceCode,
        Map<String, Object> parameters
) {
}
