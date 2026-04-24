package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem;

import java.util.List;
import java.util.Optional;

/**
 * 文件来源检查点网关。
 */
public interface TransferSourceCheckpointGateway {

    boolean existsProcessedItem(String sourceId, String itemKey);

    Optional<TransferSourceCheckpointItem> findProcessedItem(String sourceId, String itemKey);

    TransferSourceCheckpointItem saveProcessedItem(TransferSourceCheckpointItem item);

    void deleteProcessedItemsBySourceId(String sourceId);

    void deleteCheckpointsBySourceId(String sourceId);

    Optional<TransferSourceCheckpoint> findCheckpoint(String sourceId, String checkpointKey);

    TransferSourceCheckpoint saveCheckpoint(TransferSourceCheckpoint checkpoint);

    List<TransferSourceCheckpoint> listCheckpointsBySourceId(String sourceId, Integer limit);

    List<TransferSourceCheckpointItem> listProcessedItemsBySourceId(String sourceId, Integer limit);
}
