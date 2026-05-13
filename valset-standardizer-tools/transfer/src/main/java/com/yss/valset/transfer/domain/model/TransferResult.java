package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 投递执行结果。
 */
public record TransferResult(
        boolean success,
        String fileId,
        String storagePath,
        List<String> messages
) {

    public TransferResult {
    }

    public TransferResult(boolean success, String fileId, List<String> messages) {
        this(success, fileId, null, messages);
    }
}
