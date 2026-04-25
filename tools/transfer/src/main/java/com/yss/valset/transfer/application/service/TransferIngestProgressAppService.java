package com.yss.valset.transfer.application.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 文件收取进度推送服务。
 */
public interface TransferIngestProgressAppService {

    /**
     * 订阅指定来源的收取进度。
     *
     * @param sourceId 来源主键
     * @return SSE 连接
     */
    SseEmitter subscribe(String sourceId);

    /**
     * 推送状态事件。
     *
     * @param sourceId 来源主键
     * @param status 状态值
     * @param message 状态说明
     */
    default void publishStatus(String sourceId, String status, String message) {
        publishStatus(sourceId, status, message, null, null);
    }

    /**
     * 推送状态事件，并携带本轮触发信息。
     *
     * @param sourceId 来源主键
     * @param status 状态值
     * @param message 状态说明
     * @param triggerType 触发类型
     * @param triggeredAt 触发时间
     */
    void publishStatus(String sourceId, String status, String message, String triggerType, String triggeredAt);

    /**
     * 推送进度事件。
     *
     * @param sourceId 来源主键
     * @param processedCount 已处理数量
     * @param totalCount 总数量
     * @param message 进度说明
     */
    void publishProgress(String sourceId, long processedCount, long totalCount, String message);

    /**
     * 推送普通消息事件。
     *
     * @param sourceId 来源主键
     * @param message 消息内容
     */
    void publishMessage(String sourceId, String message);

    /**
     * 推送完成事件。
     *
     * @param sourceId 来源主键
     * @param message 完成说明
     */
    void publishComplete(String sourceId, String message);

    /**
     * 推送错误事件。
     *
     * @param sourceId 来源主键
     * @param code 错误码
     * @param message 错误说明
     */
    void publishError(String sourceId, String code, String message);
}
