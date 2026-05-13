package com.yss.valset.transfer.infrastructure.source.support;

import com.yss.valset.transfer.domain.model.TransferSource;
import org.slf4j.Logger;

/**
 * 来源收取日志辅助工具。
 */
public final class SourceFetchLogSupport {

    private SourceFetchLogSupport() {
    }

    public static void logStart(Logger log,
                                String sourceLabel,
                                TransferSource source,
                                String scopeLabel,
                                Object scopeValue,
                                String totalLabel,
                                long totalCount) {
        if (log == null) {
            return;
        }
        log.info("{}来源收取开始，sourceId={}，sourceCode={}，{}={}，{}={}",
                sourceLabel,
                source == null ? null : source.sourceId(),
                source == null ? null : source.sourceCode(),
                scopeLabel,
                scopeValue,
                totalLabel,
                totalCount);
    }
}
