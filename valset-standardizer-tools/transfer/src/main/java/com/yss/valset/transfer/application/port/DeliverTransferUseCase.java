package com.yss.valset.transfer.application.port;

/**
 * 文件投递用例。
 */
public interface DeliverTransferUseCase {

    void execute(String routeId, String transferId);

    void execute(String routeId, String transferId, Integer retryCount);
}
