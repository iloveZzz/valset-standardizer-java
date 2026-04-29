package com.yss.valset.analysis.domain.gateway;

import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.analysis.domain.model.ParseQueuePage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 待解析任务网关。
 */
public interface ParseQueueGateway {

    Optional<ParseQueue> findById(String queueId);

    Optional<ParseQueue> findByBusinessKey(String businessKey);

    ParseQueuePage pageQueues(String transferId,
                                      String businessKey,
                                      String sourceCode,
                                      String routeId,
                                      String tagCode,
                                      String fileStatus,
                                      String deliveryStatus,
                                      String parseStatus,
                                      String triggerMode,
                                      Integer pageIndex,
                                      Integer pageSize);

    List<ParseQueue> listQueues(String transferId,
                                        String businessKey,
                                        String sourceCode,
                                        String routeId,
                                        String tagCode,
                                        String fileStatus,
                                        String deliveryStatus,
                                        String parseStatus,
                                        String triggerMode,
                                        Integer limit);

    /**
     * 按创建时间从早到晚查询待订阅事件。
     */
    List<ParseQueue> listPendingQueues(Integer limit);

    /**
     * 仅在待订阅状态时接管队列。
     */
    boolean subscribeIfPending(String queueId, String subscribedBy, Instant subscribedAt);

    ParseQueue save(ParseQueue queue);
}
