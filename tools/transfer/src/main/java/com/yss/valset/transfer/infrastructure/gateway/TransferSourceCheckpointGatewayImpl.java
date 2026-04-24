package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferSourceCheckpointItemPO;
import com.yss.valset.transfer.infrastructure.entity.TransferSourceCheckpointPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferSourceCheckpointItemRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferSourceCheckpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MyBatis 支持的来源检查点网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferSourceCheckpointGatewayImpl implements TransferSourceCheckpointGateway {

    private final TransferSourceCheckpointRepository transferSourceCheckpointRepository;
    private final TransferSourceCheckpointItemRepository transferSourceCheckpointItemRepository;
    private final TransferJsonMapper transferJsonMapper;

    @Override
    public boolean existsProcessedItem(String sourceId, String itemKey) {
        if (sourceId == null || sourceId.isBlank() || itemKey == null || itemKey.isBlank()) {
            return false;
        }
        return transferSourceCheckpointItemRepository.selectCount(
                Wrappers.lambdaQuery(TransferSourceCheckpointItemPO.class)
                        .eq(TransferSourceCheckpointItemPO::getSourceId, sourceId)
                        .eq(TransferSourceCheckpointItemPO::getItemKey, itemKey)
        ) > 0;
    }

    @Override
    public Optional<TransferSourceCheckpointItem> findProcessedItem(String sourceId, String itemKey) {
        if (sourceId == null || sourceId.isBlank() || itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(transferSourceCheckpointItemRepository.selectOne(
                        Wrappers.lambdaQuery(TransferSourceCheckpointItemPO.class)
                                .eq(TransferSourceCheckpointItemPO::getSourceId, sourceId)
                                .eq(TransferSourceCheckpointItemPO::getItemKey, itemKey)
                                .last("limit 1")
                ))
                .map(this::toDomain);
    }

    @Override
    public TransferSourceCheckpointItem saveProcessedItem(TransferSourceCheckpointItem item) {
        TransferSourceCheckpointItemPO po = toPO(item);
        Instant now = Instant.now();
        if (po.getProcessedAt() == null) {
            po.setProcessedAt(toLocalDateTime(now));
        }
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(toLocalDateTime(now));
        }
        po.setUpdatedAt(toLocalDateTime(now));
        TransferSourceCheckpointItemPO existing = transferSourceCheckpointItemRepository.selectOne(
                Wrappers.lambdaQuery(TransferSourceCheckpointItemPO.class)
                        .eq(TransferSourceCheckpointItemPO::getSourceId, po.getSourceId())
                        .eq(TransferSourceCheckpointItemPO::getItemKey, po.getItemKey())
                        .last("limit 1")
        );
        try {
            if (existing == null) {
                transferSourceCheckpointItemRepository.insert(po);
            } else {
                po.setCheckpointItemId(existing.getCheckpointItemId());
                po.setCreatedAt(existing.getCreatedAt());
                transferSourceCheckpointItemRepository.updateById(po);
            }
        } catch (DuplicateKeyException ex) {
            TransferSourceCheckpointItemPO conflict = transferSourceCheckpointItemRepository.selectOne(
                    Wrappers.lambdaQuery(TransferSourceCheckpointItemPO.class)
                            .eq(TransferSourceCheckpointItemPO::getSourceId, po.getSourceId())
                            .eq(TransferSourceCheckpointItemPO::getItemKey, po.getItemKey())
                            .last("limit 1")
            );
            if (conflict != null) {
                return toDomain(conflict);
            }
            throw ex;
        }
        return toDomain(po);
    }

    @Override
    public void deleteProcessedItemsBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return;
        }
        transferSourceCheckpointItemRepository.delete(
                Wrappers.lambdaQuery(TransferSourceCheckpointItemPO.class)
                        .eq(TransferSourceCheckpointItemPO::getSourceId, sourceId)
        );
    }

    @Override
    public void deleteCheckpointsBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return;
        }
        transferSourceCheckpointRepository.delete(
                Wrappers.lambdaQuery(TransferSourceCheckpointPO.class)
                        .eq(TransferSourceCheckpointPO::getSourceId, sourceId)
        );
    }

    @Override
    public Optional<TransferSourceCheckpoint> findCheckpoint(String sourceId, String checkpointKey) {
        if (sourceId == null || sourceId.isBlank() || checkpointKey == null || checkpointKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(transferSourceCheckpointRepository.selectOne(
                        Wrappers.lambdaQuery(TransferSourceCheckpointPO.class)
                                .eq(TransferSourceCheckpointPO::getSourceId, sourceId)
                                .eq(TransferSourceCheckpointPO::getCheckpointKey, checkpointKey)
                                .last("limit 1")
                ))
                .map(this::toDomain);
    }

    @Override
    public TransferSourceCheckpoint saveCheckpoint(TransferSourceCheckpoint checkpoint) {
        TransferSourceCheckpointPO po = toPO(checkpoint);
        Instant now = Instant.now();
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(toLocalDateTime(now));
        }
        po.setUpdatedAt(toLocalDateTime(now));
        TransferSourceCheckpointPO existing = transferSourceCheckpointRepository.selectOne(
                Wrappers.lambdaQuery(TransferSourceCheckpointPO.class)
                        .eq(TransferSourceCheckpointPO::getSourceId, po.getSourceId())
                        .eq(TransferSourceCheckpointPO::getCheckpointKey, po.getCheckpointKey())
                        .last("limit 1")
        );
        try {
            if (existing == null) {
                transferSourceCheckpointRepository.insert(po);
            } else {
                po.setCheckpointId(existing.getCheckpointId());
                po.setCreatedAt(existing.getCreatedAt());
                transferSourceCheckpointRepository.updateById(po);
            }
        } catch (DuplicateKeyException ex) {
            TransferSourceCheckpointPO conflict = transferSourceCheckpointRepository.selectOne(
                    Wrappers.lambdaQuery(TransferSourceCheckpointPO.class)
                            .eq(TransferSourceCheckpointPO::getSourceId, po.getSourceId())
                            .eq(TransferSourceCheckpointPO::getCheckpointKey, po.getCheckpointKey())
                            .last("limit 1")
            );
            if (conflict != null) {
                return toDomain(conflict);
            }
            throw ex;
        }
        return toDomain(po);
    }

    @Override
    public List<TransferSourceCheckpoint> listCheckpointsBySourceId(String sourceId, Integer limit) {
        if (sourceId == null || sourceId.isBlank()) {
            return List.of();
        }
        var query = Wrappers.lambdaQuery(TransferSourceCheckpointPO.class)
                .eq(TransferSourceCheckpointPO::getSourceId, sourceId)
                .orderByDesc(TransferSourceCheckpointPO::getUpdatedAt);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferSourceCheckpointRepository.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TransferSourceCheckpointItem> listProcessedItemsBySourceId(String sourceId, Integer limit) {
        if (sourceId == null || sourceId.isBlank()) {
            return List.of();
        }
        var query = Wrappers.lambdaQuery(TransferSourceCheckpointItemPO.class)
                .eq(TransferSourceCheckpointItemPO::getSourceId, sourceId)
                .orderByDesc(TransferSourceCheckpointItemPO::getProcessedAt);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferSourceCheckpointItemRepository.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    private TransferSourceCheckpointItemPO toPO(TransferSourceCheckpointItem item) {
        if (item == null) {
            return null;
        }
        TransferSourceCheckpointItemPO po = new TransferSourceCheckpointItemPO();
        po.setCheckpointItemId(item.checkpointItemId());
        po.setSourceId(item.sourceId());
        po.setSourceType(item.sourceType());
        po.setItemKey(item.itemKey());
        po.setItemRef(item.itemRef());
        po.setItemName(item.itemName());
        po.setItemSize(item.itemSize());
        po.setItemMimeType(item.itemMimeType());
        po.setItemFingerprint(item.itemFingerprint());
        po.setItemMetaJson(transferJsonMapper.toJson(item.itemMeta()));
        po.setTriggerType(item.triggerType());
        po.setProcessedAt(item.processedAt() == null ? null : toLocalDateTime(item.processedAt()));
        po.setCreatedAt(item.createdAt() == null ? null : toLocalDateTime(item.createdAt()));
        po.setUpdatedAt(item.updatedAt() == null ? null : toLocalDateTime(item.updatedAt()));
        return po;
    }

    private TransferSourceCheckpointItem toDomain(TransferSourceCheckpointItemPO po) {
        if (po == null) {
            return null;
        }
        return new TransferSourceCheckpointItem(
                po.getCheckpointItemId(),
                po.getSourceId(),
                po.getSourceType(),
                po.getItemKey(),
                po.getItemRef(),
                po.getItemName(),
                po.getItemSize(),
                po.getItemMimeType(),
                po.getItemFingerprint(),
                transferJsonMapper.toMap(po.getItemMetaJson()),
                po.getTriggerType(),
                toInstant(po.getProcessedAt()),
                toInstant(po.getCreatedAt()),
                toInstant(po.getUpdatedAt())
        );
    }

    private TransferSourceCheckpointPO toPO(TransferSourceCheckpoint checkpoint) {
        if (checkpoint == null) {
            return null;
        }
        TransferSourceCheckpointPO po = new TransferSourceCheckpointPO();
        po.setCheckpointId(checkpoint.checkpointId());
        po.setSourceId(checkpoint.sourceId());
        po.setSourceType(checkpoint.sourceType());
        po.setCheckpointKey(checkpoint.checkpointKey());
        po.setCheckpointValue(checkpoint.checkpointValue());
        po.setCheckpointJson(transferJsonMapper.toJson(checkpoint.checkpointMeta()));
        po.setCreatedAt(checkpoint.createdAt() == null ? null : toLocalDateTime(checkpoint.createdAt()));
        po.setUpdatedAt(checkpoint.updatedAt() == null ? null : toLocalDateTime(checkpoint.updatedAt()));
        return po;
    }

    private TransferSourceCheckpoint toDomain(TransferSourceCheckpointPO po) {
        if (po == null) {
            return null;
        }
        return new TransferSourceCheckpoint(
                po.getCheckpointId(),
                po.getSourceId(),
                po.getSourceType(),
                po.getCheckpointKey(),
                po.getCheckpointValue(),
                transferJsonMapper.toMap(po.getCheckpointJson()),
                toInstant(po.getCreatedAt()),
                toInstant(po.getUpdatedAt())
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
