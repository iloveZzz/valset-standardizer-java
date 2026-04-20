package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.application.port.DeliverTransferUseCase;
import com.yss.valset.transfer.application.port.RouteTransferUseCase;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 默认文件收发分拣总应用服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferProcessService implements TransferProcessUseCase {

    private final DefaultIngestTransferService ingestTransferService;
    private final RouteTransferUseCase routeTransferUseCase;
    private final DeliverTransferUseCase deliverTransferUseCase;

    @Override
    public void ingest(IngestTransferSourceCommand command) {
        ingestTransferService.execute(command);
    }

    @Override
    public void route(Long transferId) {
        routeTransferUseCase.execute(transferId);
    }

    @Override
    public void deliver(Long routeId) {
        deliverTransferUseCase.execute(routeId);
    }
}
