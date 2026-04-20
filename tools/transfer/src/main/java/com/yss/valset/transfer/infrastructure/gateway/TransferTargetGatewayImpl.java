package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferTargetPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis 支持的投递目标网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferTargetGatewayImpl implements TransferTargetGateway {

    private final TransferTargetRepository transferTargetRepository;
    private final TransferJsonMapper transferJsonMapper;

    @Override
    public Optional<TransferTarget> findById(Long targetId) {
        return Optional.ofNullable(transferTargetRepository.selectById(targetId)).map(this::toDomain);
    }

    @Override
    public Optional<TransferTarget> findByTargetCode(String targetCode) {
        TransferTargetPO po = transferTargetRepository.selectOne(
                Wrappers.lambdaQuery(TransferTargetPO.class)
                        .eq(TransferTargetPO::getTargetCode, targetCode)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TransferTarget> listEnabledTargets() {
        return transferTargetRepository.selectList(
                        Wrappers.lambdaQuery(TransferTargetPO.class)
                                .eq(TransferTargetPO::getEnabled, Boolean.TRUE)
                                .orderByAsc(TransferTargetPO::getTargetType)
                                .orderByAsc(TransferTargetPO::getTargetCode)
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TransferTarget save(TransferTarget transferTarget) {
        TransferTargetPO po = toPO(transferTarget);
        if (po.getTargetId() == null) {
            transferTargetRepository.insert(po);
        } else {
            transferTargetRepository.updateById(po);
        }
        return toDomain(po);
    }

    private TransferTargetPO toPO(TransferTarget transferTarget) {
        TransferTargetPO po = new TransferTargetPO();
        po.setTargetId(transferTarget.targetId());
        po.setTargetCode(transferTarget.targetCode());
        po.setTargetName(transferTarget.targetName());
        po.setTargetType(transferTarget.targetType() == null ? null : transferTarget.targetType().name());
        po.setEnabled(transferTarget.enabled());
        po.setTargetPathTemplate(transferTarget.targetPathTemplate());
        po.setConnectionConfigJson(transferJsonMapper.toJson(transferTarget.connectionConfig()));
        po.setTargetMetaJson(transferJsonMapper.toJson(transferTarget.targetMeta()));
        return po;
    }

    private TransferTarget toDomain(TransferTargetPO po) {
        return new TransferTarget(
                po.getTargetId(),
                po.getTargetCode(),
                po.getTargetName(),
                po.getTargetType() == null ? null : TargetType.valueOf(po.getTargetType()),
                Boolean.TRUE.equals(po.getEnabled()),
                po.getTargetPathTemplate(),
                transferJsonMapper.toMap(po.getConnectionConfigJson()),
                transferJsonMapper.toMap(po.getTargetMetaJson())
        );
    }
}
