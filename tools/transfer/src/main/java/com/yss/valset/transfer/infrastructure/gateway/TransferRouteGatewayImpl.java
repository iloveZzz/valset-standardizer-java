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
    public Optional<TransferRoute> findById(String routeId) {
        return Optional.ofNullable(transferRouteRepository.selectById(parseLong(routeId))).map(this::toDomain);
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
    public long countBySourceId(String sourceId) {
        Long id = parseLong(sourceId);
        if (id == null) {
            return 0L;
        }
        return transferRouteRepository.selectCount(
                Wrappers.lambdaQuery(TransferRoutePO.class)
                        .eq(TransferRoutePO::getSourceId, id)
        );
    }

    @Override
    public long countByTargetCode(String targetCode) {
        if (targetCode == null || targetCode.isBlank()) {
            return 0L;
        }
        return transferRouteRepository.selectCount(
                Wrappers.lambdaQuery(TransferRoutePO.class)
                        .eq(TransferRoutePO::getTargetCode, targetCode)
        );
    }

    @Override
    public List<TransferRoute> listRoutes(String sourceId,
                                          String sourceType,
                                          String sourceCode,
                                          String ruleId,
                                          String targetType,
                                          String targetCode,
                                          String routeStatus,
                                          Integer limit) {
        Long sourceIdValue = parseLong(sourceId);
        Long ruleIdValue = parseLong(ruleId);
        List<TransferRoute> routes = transferRouteRepository.selectList(
                        Wrappers.lambdaQuery(TransferRoutePO.class)
                                .eq(sourceIdValue != null, TransferRoutePO::getSourceId, sourceIdValue)
                                .eq(sourceType != null && !sourceType.isBlank(), TransferRoutePO::getSourceType, sourceType)
                                .eq(sourceCode != null && !sourceCode.isBlank(), TransferRoutePO::getSourceCode, sourceCode)
                                .eq(ruleIdValue != null, TransferRoutePO::getRuleId, ruleIdValue)
                                .eq(targetType != null && !targetType.isBlank(), TransferRoutePO::getTargetType, targetType)
                                .eq(targetCode != null && !targetCode.isBlank(), TransferRoutePO::getTargetCode, targetCode)
                                .eq(routeStatus != null && !routeStatus.isBlank(), TransferRoutePO::getRouteStatus, routeStatus)
                                .orderByDesc(TransferRoutePO::getRouteId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
        if (limit == null || limit <= 0 || routes.size() <= limit) {
            return routes;
        }
        return routes.subList(0, limit);
    }

    @Override
    public void deleteById(String routeId) {
        Long id = parseLong(routeId);
        if (id != null) {
            transferRouteRepository.deleteById(id);
        }
    }

    private TransferRoutePO toPO(TransferRoute transferRoute) {
        return transferRouteMapper.toPO(transferRoute);
    }

    private TransferRoute toDomain(TransferRoutePO po) {
        return transferRouteMapper.toDomain(po);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
