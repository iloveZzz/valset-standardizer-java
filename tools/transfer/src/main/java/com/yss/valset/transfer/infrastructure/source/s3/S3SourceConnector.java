package com.yss.valset.transfer.infrastructure.source.s3;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * S3 来源连接器。
 */
@Component
public class S3SourceConnector implements SourceConnector {

    @Override
    public String type() {
        return SourceType.S3.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.S3;
    }

    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        return Collections.emptyList();
    }
}
