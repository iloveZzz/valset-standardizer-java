package com.yss.valset.transfer.domain.plugin;

import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;

/**
 * 文件投递动作插件。
 */
public interface TransferActionPlugin {

    String type();

    int priority();

    boolean supports(TransferRoute route);

    TransferResult execute(TransferContext context);
}
