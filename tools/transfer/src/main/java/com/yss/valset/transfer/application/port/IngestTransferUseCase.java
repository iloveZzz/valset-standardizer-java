package com.yss.valset.transfer.application.port;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;

/**
 * 收取文件用例。
 */
public interface IngestTransferUseCase {

    void execute(IngestTransferSourceCommand command);
}
