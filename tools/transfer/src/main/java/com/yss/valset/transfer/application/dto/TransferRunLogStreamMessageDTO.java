package com.yss.valset.transfer.application.dto;

/**
 * 文件收发运行日志 SSE 消息。
 */
public record TransferRunLogStreamMessageDTO(
        String type,
        String taskId,
        TransferRunLogViewDTO data
) {
}
