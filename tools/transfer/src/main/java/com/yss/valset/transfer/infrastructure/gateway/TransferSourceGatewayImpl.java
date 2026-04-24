package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.infrastructure.convertor.TransferSourceMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferSourcePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MyBatis 支持的文件来源网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferSourceGatewayImpl implements TransferSourceGateway {

    private final TransferSourceRepository transferSourceRepository;
    private final TransferSourceMapper transferSourceMapper;

    @Override
    public Optional<TransferSource> findById(String sourceId) {
        return Optional.ofNullable(transferSourceRepository.selectById(parseLong(sourceId))).map(this::toDomain);
    }

    @Override
    public Optional<TransferSource> findBySourceCode(String sourceCode) {
        TransferSourcePO po = transferSourceRepository.selectOne(
                Wrappers.lambdaQuery(TransferSourcePO.class)
                        .eq(TransferSourcePO::getSourceCode, sourceCode)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TransferSource> listEnabledSources() {
        return listSources(null, null, null, Boolean.TRUE, null);
    }

    @Override
    public List<TransferSource> listSources(String sourceType, String sourceCode, String sourceName, Boolean enabled, Integer limit) {
        var query = Wrappers.lambdaQuery(TransferSourcePO.class)
                .eq(sourceType != null && !sourceType.isBlank(), TransferSourcePO::getSourceType, sourceType)
                .like(sourceCode != null && !sourceCode.isBlank(), TransferSourcePO::getSourceCode, sourceCode)
                .like(sourceName != null && !sourceName.isBlank(), TransferSourcePO::getSourceName, sourceName)
                .eq(enabled != null, TransferSourcePO::getEnabled, enabled)
                .orderByAsc(TransferSourcePO::getSourceType)
                .orderByAsc(TransferSourcePO::getSourceCode);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferSourceRepository.selectList(query)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TransferSource save(TransferSource transferSource) {
        TransferSourcePO po = toPO(transferSource);
        LocalDateTime now = LocalDateTime.now();
        if (po.getSourceId() == null) {
            po.setCreatedAt(now);
            po.setUpdatedAt(now);
            transferSourceRepository.insert(po);
        } else {
            TransferSourcePO existing = transferSourceRepository.selectById(po.getSourceId());
            po.setCreatedAt(existing == null ? now : existing.getCreatedAt());
            po.setUpdatedAt(now);
            transferSourceRepository.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public void deleteById(String sourceId) {
        Long id = parseLong(sourceId);
        if (id != null) {
            transferSourceRepository.deleteById(id);
        }
    }

    @Override
    public boolean tryAcquireIngestLock(String sourceId, String lockToken, Instant startedAt, String triggerType) {
        Long id = parseLong(sourceId);
        if (id == null || startedAt == null) {
            return false;
        }
        int updated = transferSourceRepository.update(
                null,
                Wrappers.lambdaUpdate(TransferSourcePO.class)
                        .eq(TransferSourcePO::getSourceId, id)
                        .and(wrapper -> wrapper.isNull(TransferSourcePO::getIngestStatus)
                                .or()
                                .eq(TransferSourcePO::getIngestStatus, "IDLE")
                                .or()
                                .eq(TransferSourcePO::getIngestStatus, "STOPPED"))
                        .set(TransferSourcePO::getIngestStatus, "RUNNING")
                        .set(TransferSourcePO::getIngestTriggerType, triggerType)
                        .set(TransferSourcePO::getIngestStartedAt, toLocalDateTime(startedAt))
                        .set(TransferSourcePO::getIngestFinishedAt, null)
                        .set(TransferSourcePO::getUpdatedAt, toLocalDateTime(startedAt))
        );
        return updated > 0;
    }

    @Override
    public boolean requestIngestStop(String sourceId, Instant requestedAt) {
        Long id = parseLong(sourceId);
        if (id == null) {
            return false;
        }
        int updated = transferSourceRepository.update(
                null,
                Wrappers.lambdaUpdate(TransferSourcePO.class)
                        .eq(TransferSourcePO::getSourceId, id)
                        .and(wrapper -> wrapper.eq(TransferSourcePO::getIngestStatus, "RUNNING")
                                .or()
                                .eq(TransferSourcePO::getIngestStatus, "STOPPING")
                                .or()
                                .eq(TransferSourcePO::getIngestStatus, "STOPPED"))
                        .set(TransferSourcePO::getIngestStatus, "STOPPED")
                        .set(TransferSourcePO::getIngestFinishedAt, toLocalDateTime(requestedAt))
                        .set(TransferSourcePO::getUpdatedAt, toLocalDateTime(requestedAt))
        );
        return updated > 0;
    }

    @Override
    public boolean forceStopIngest(String sourceId, Instant finishedAt) {
        Long id = parseLong(sourceId);
        if (id == null) {
            return false;
        }
        int updated = transferSourceRepository.update(
                null,
                Wrappers.lambdaUpdate(TransferSourcePO.class)
                        .eq(TransferSourcePO::getSourceId, id)
                        .and(wrapper -> wrapper.eq(TransferSourcePO::getIngestStatus, "RUNNING")
                                .or()
                                .eq(TransferSourcePO::getIngestStatus, "STOPPING")
                                .or()
                                .eq(TransferSourcePO::getIngestStatus, "STOPPED"))
                        .set(TransferSourcePO::getIngestStatus, "STOPPED")
                        .set(TransferSourcePO::getIngestFinishedAt, toLocalDateTime(finishedAt))
                        .set(TransferSourcePO::getUpdatedAt, toLocalDateTime(finishedAt))
        );
        return updated > 0;
    }

    @Override
    public boolean releaseIngestLock(String sourceId, String lockToken, Instant finishedAt) {
        Long id = parseLong(sourceId);
        if (id == null) {
            return false;
        }
        TransferSourcePO current = transferSourceRepository.selectById(id);
        if (current != null && "STOPPED".equalsIgnoreCase(current.getIngestStatus())) {
            return true;
        }
        String finalStatus = current != null && "STOPPING".equalsIgnoreCase(current.getIngestStatus())
                ? "STOPPED"
                : "IDLE";
        int updated = transferSourceRepository.update(
                null,
                Wrappers.lambdaUpdate(TransferSourcePO.class)
                        .eq(TransferSourcePO::getSourceId, id)
                        .set(TransferSourcePO::getIngestStatus, finalStatus)
                        .set(TransferSourcePO::getIngestFinishedAt, toLocalDateTime(finishedAt))
                        .set(TransferSourcePO::getUpdatedAt, toLocalDateTime(finishedAt))
        );
        return updated > 0;
    }

    private TransferSourcePO toPO(TransferSource transferSource) {
        return transferSourceMapper.toPO(transferSource);
    }

    private TransferSource toDomain(TransferSourcePO po) {
        return transferSourceMapper.toDomain(po);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
