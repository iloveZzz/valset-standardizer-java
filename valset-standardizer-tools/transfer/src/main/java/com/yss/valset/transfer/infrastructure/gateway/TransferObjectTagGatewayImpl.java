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
        if (transferId == null || transferId.trim().isEmpty()) {
            return java.util.Arrays.asList();
        }
        return listByTransferIds(java.util.Arrays.asList(transferId));
    }

    @Override
    public List<TransferObjectTag> listByTransferIds(List<String> transferIds) {
        if (transferIds == null || transferIds.isEmpty()) {
            return java.util.Arrays.asList();
        }
        List<String> normalizedTransferIds = transferIds.stream()
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
        if (normalizedTransferIds.isEmpty()) {
            return java.util.Arrays.asList();
        }
        return transferObjectTagRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectTagPO.class)
                                .in(TransferObjectTagPO::getTransferId, normalizedTransferIds)
                                .orderByAsc(TransferObjectTagPO::getTransferId)
                                .orderByAsc(TransferObjectTagPO::getCreatedAt)
                )
                .stream()
                .map(transferObjectTagMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<TransferObjectTag> saveAll(List<TransferObjectTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return java.util.Arrays.asList();
        }
        List<TransferObjectTagPO> pos = tags.stream().map(tag -> {
            TransferObjectTagPO po = transferObjectTagMapper.toPO(tag);
            if (po.getCreatedAt() == null) {
                po.setCreatedAt(LocalDateTime.now());
            }
            return po;
        }).collect(java.util.stream.Collectors.toList());
        for (TransferObjectTagPO po : pos) {
            transferObjectTagRepository.insert(po);
        }
        return pos.stream().map(transferObjectTagMapper::toDomain).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteByTransferId(String transferId) {
        transferObjectTagRepository.delete(
                Wrappers.lambdaQuery(TransferObjectTagPO.class)
                        .eq(TransferObjectTagPO::getTransferId, transferId)
        );
    }
}
