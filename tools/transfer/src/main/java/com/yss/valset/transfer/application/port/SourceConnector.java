package com.yss.valset.transfer.application.port;

import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.List;

/**
 * 文件来源连接器。
 */
public interface SourceConnector {

    String type();

    boolean supports(TransferSource source);

    List<RecognitionContext> fetch(TransferSource source);
}
