package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.infrastructure.convertor.TransferTargetMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferTargetPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MyBatis 支持的投递目标网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferTargetGatewayImpl implements TransferTargetGateway {

    private final TransferTargetRepository transferTargetRepository;
    private final TransferTargetMapper transferTargetMapper;

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
        return listTargets(null, null, Boolean.TRUE, null);
    }

    @Override
    public List<TransferTarget> listTargets(String targetType, String targetCode, Boolean enabled, Integer limit) {
        var query = Wrappers.lambdaQuery(TransferTargetPO.class)
                .eq(targetType != null && !targetType.isBlank(), TransferTargetPO::getTargetType, targetType)
                .like(targetCode != null && !targetCode.isBlank(), TransferTargetPO::getTargetCode, targetCode)
                .eq(enabled != null, TransferTargetPO::getEnabled, enabled)
                .orderByAsc(TransferTargetPO::getTargetType)
                .orderByAsc(TransferTargetPO::getTargetCode);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferTargetRepository.selectList(query)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TransferTarget save(TransferTarget transferTarget) {
        TransferTargetPO po = toPO(transferTarget);
        LocalDateTime now = LocalDateTime.now();
        if (po.getTargetId() == null) {
            po.setCreatedAt(now);
            po.setUpdatedAt(now);
            transferTargetRepository.insert(po);
        } else {
            TransferTargetPO existing = transferTargetRepository.selectById(po.getTargetId());
            po.setCreatedAt(existing == null ? now : existing.getCreatedAt());
            po.setUpdatedAt(now);
            transferTargetRepository.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public void deleteById(Long targetId) {
        if (targetId != null) {
            transferTargetRepository.deleteById(targetId);
        }
    }

    private TransferTargetPO toPO(TransferTarget transferTarget) {
        return transferTargetMapper.toPO(transferTarget);
    }

    private TransferTarget toDomain(TransferTargetPO po) {
        return transferTargetMapper.toDomain(po);
    }
}
