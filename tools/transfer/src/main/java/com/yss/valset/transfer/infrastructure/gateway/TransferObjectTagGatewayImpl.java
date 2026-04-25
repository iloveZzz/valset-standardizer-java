package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.infrastructure.convertor.TransferObjectTagMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectTagPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis 支持的文件对象标签结果网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferObjectTagGatewayImpl implements TransferObjectTagGateway {

    private final TransferObjectTagRepository transferObjectTagRepository;
    private final TransferObjectTagMapper transferObjectTagMapper;

    @Override
    public List<TransferObjectTag> listByTransferId(String transferId) {
        if (transferId == null || transferId.isBlank()) {
            return List.of();
        }
        return listByTransferIds(List.of(transferId));
    }

    @Override
    public List<TransferObjectTag> listByTransferIds(List<String> transferIds) {
        if (transferIds == null || transferIds.isEmpty()) {
            return List.of();
        }
        List<String> normalizedTransferIds = transferIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (normalizedTransferIds.isEmpty()) {
            return List.of();
        }
        return transferObjectTagRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectTagPO.class)
                                .in(TransferObjectTagPO::getTransferId, normalizedTransferIds)
                                .orderByAsc(TransferObjectTagPO::getTransferId)
                                .orderByAsc(TransferObjectTagPO::getCreatedAt)
                )
                .stream()
                .map(transferObjectTagMapper::toDomain)
                .toList();
    }

    @Override
    public List<TransferObjectTag> saveAll(List<TransferObjectTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<TransferObjectTagPO> pos = tags.stream().map(tag -> {
            TransferObjectTagPO po = transferObjectTagMapper.toPO(tag);
            if (po.getCreatedAt() == null) {
                po.setCreatedAt(LocalDateTime.now());
            }
            return po;
        }).toList();
        for (TransferObjectTagPO po : pos) {
            transferObjectTagRepository.insert(po);
        }
        return pos.stream().map(transferObjectTagMapper::toDomain).toList();
    }

    @Override
    public void deleteByTransferId(String transferId) {
        transferObjectTagRepository.delete(
                Wrappers.lambdaQuery(TransferObjectTagPO.class)
                        .eq(TransferObjectTagPO::getTransferId, transferId)
        );
    }
}
