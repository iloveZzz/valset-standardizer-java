package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.infrastructure.convertor.TransferRouteMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferRoutePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis 支持的文件路由网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferRouteGatewayImpl implements TransferRouteGateway {

    private final TransferRouteRepository transferRouteRepository;
    private final TransferRouteMapper transferRouteMapper;

    @Override
    public Optional<TransferRoute> findById(Long routeId) {
        return Optional.ofNullable(transferRouteRepository.selectById(routeId)).map(this::toDomain);
    }

    @Override
    public TransferRoute save(TransferRoute transferRoute) {
        TransferRoutePO po = toPO(transferRoute);
        if (po.getRouteId() == null) {
            transferRouteRepository.insert(po);
        } else {
            transferRouteRepository.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public List<TransferRoute> listByTransferId(Long transferId) {
        return transferRouteRepository.selectList(
                        Wrappers.lambdaQuery(TransferRoutePO.class)
                                .eq(TransferRoutePO::getTransferId, transferId)
                                .orderByAsc(TransferRoutePO::getRouteId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TransferRoutePO toPO(TransferRoute transferRoute) {
        return transferRouteMapper.toPO(transferRoute);
    }

    private TransferRoute toDomain(TransferRoutePO po) {
        return transferRouteMapper.toDomain(po);
    }
}
