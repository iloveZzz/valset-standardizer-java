package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.convertor.TransferObjectMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MyBatis 支持的文件主对象网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferObjectGatewayImpl implements TransferObjectGateway {

    private final TransferObjectRepository transferObjectRepository;
    private final TransferJsonMapper transferJsonMapper;
    private final TransferObjectMapper transferObjectMapper;

    @Override
    public Optional<TransferObject> findById(Long transferId) {
        TransferObjectPO po = transferObjectRepository.selectById(transferId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TransferObject> findByFingerprint(String fingerprint) {
        TransferObjectPO po = transferObjectRepository.selectOne(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(TransferObjectPO::getFingerprint, fingerprint)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public TransferObject save(TransferObject transferObject) {
        TransferObjectPO po = toPO(transferObject);
        if (po.getTransferId() == null) {
            transferObjectRepository.insert(po);
        } else {
            transferObjectRepository.updateById(po);
        }
        return toDomain(po);
    }

    private TransferObjectPO toPO(TransferObject transferObject) {
        return transferObjectMapper.toPO(transferObject, transferJsonMapper);
    }

    private TransferObject toDomain(TransferObjectPO po) {
        return transferObjectMapper.toDomain(po, transferJsonMapper);
    }
}
