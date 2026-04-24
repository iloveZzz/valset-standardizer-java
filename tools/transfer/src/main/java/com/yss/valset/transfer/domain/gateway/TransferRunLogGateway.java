package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunLogAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogPage;

import java.util.List;
import java.util.Optional;

/**
 * 文件收发运行日志网关。
 */
public interface TransferRunLogGateway {

    TransferRunLog save(TransferRunLog transferRunLog);

    Optional<TransferRunLog> findById(String runLogId);

    long deleteFailedDeliverLogsByTransferId(String transferId);

    List<TransferRunLog> listLogs(String sourceId,
                                  String transferId,
                                  String routeId,
                                  String runStage,
                                  String runStatus,
                                  String triggerType,
                                  Integer limit);

    TransferRunLogPage pageLogs(String sourceId,
                                String transferId,
                                String routeId,
                                String runStage,
                                String runStatus,
                                String triggerType,
                                String keyword,
                                Integer pageIndex,
                                Integer pageSize);

    TransferRunLogAnalysis analyzeLogs(String sourceId,
                                       String transferId,
                                       String routeId,
                                       String runStage,
                                       String runStatus,
                                       String triggerType,
                                       String keyword);
}
