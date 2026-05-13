package com.yss.valset.transfer.application.port;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;

/**
 * 文件收发分拣总用例。
 */
public interface TransferProcessUseCase {

    void ingest(IngestTransferSourceCommand command);

    void route(String transferId);

    void deliver(String routeId, String transferId);
}
