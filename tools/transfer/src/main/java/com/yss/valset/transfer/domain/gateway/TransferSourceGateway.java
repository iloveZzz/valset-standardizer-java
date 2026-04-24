package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 文件来源网关。
 */
public interface TransferSourceGateway {

    Optional<TransferSource> findById(String sourceId);

    Optional<TransferSource> findBySourceCode(String sourceCode);

    List<TransferSource> listEnabledSources();

    List<TransferSource> listSources(String sourceType, String sourceCode, String sourceName, Boolean enabled, Integer limit);

    TransferSource save(TransferSource transferSource);

    void deleteById(String sourceId);

    boolean tryAcquireIngestLock(String sourceId, String lockToken, Instant startedAt, String triggerType);

    boolean requestIngestStop(String sourceId, Instant requestedAt);

    boolean forceStopIngest(String sourceId, Instant finishedAt);

    boolean releaseIngestLock(String sourceId, String lockToken, Instant finishedAt);
}
