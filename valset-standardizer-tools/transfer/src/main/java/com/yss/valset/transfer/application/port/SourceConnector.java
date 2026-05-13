package com.yss.valset.transfer.application.port;

import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferSource;

import java.nio.file.Path;
import java.util.List;

/**
 * 文件来源连接器。
 */
public interface SourceConnector {

    String type();

    boolean supports(TransferSource source);

    List<RecognitionContext> fetch(TransferSource source);

    default Path materialize(TransferSource source, TransferObject transferObject) {
        throw new UnsupportedOperationException("当前来源不支持按需落盘，sourceType="
                + (source == null || source.sourceType() == null ? "UNKNOWN" : source.sourceType().name()));
    }
}
