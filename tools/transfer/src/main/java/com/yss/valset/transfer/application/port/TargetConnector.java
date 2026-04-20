package com.yss.valset.transfer.application.port;

import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.TransferResult;

/**
 * 文件投递连接器。
 */
public interface TargetConnector {

    String type();

    boolean supports(TransferTarget target);

    TransferResult send(TransferContext context);
}
