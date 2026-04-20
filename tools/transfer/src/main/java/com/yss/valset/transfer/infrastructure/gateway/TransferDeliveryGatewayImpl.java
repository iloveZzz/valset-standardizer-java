package com.yss.valset.transfer.infrastructure.gateway;

import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferDeliveryRecordPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferDeliveryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * MyBatis 支持的文件投递结果网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferDeliveryGatewayImpl implements TransferDeliveryGateway {

    private final TransferDeliveryRecordRepository transferDeliveryRecordRepository;
    private final TransferRouteGateway transferRouteGateway;
    private final TransferJsonMapper transferJsonMapper;

    @Override
    public void recordResult(Long routeId, TransferResult transferResult) {
        recordResult(routeId, transferResult, 0);
    }

    @Override
    public void recordResult(Long routeId, TransferResult transferResult, Integer retryCount) {
        TransferRoute route = transferRouteGateway.findById(routeId).orElse(null);
        TransferDeliveryRecordPO po = new TransferDeliveryRecordPO();
        po.setRouteId(routeId);
        po.setTransferId(route == null ? null : route.transferId());
        po.setTargetType(route == null || route.targetType() == null ? null : route.targetType().name());
        po.setTargetCode(route == null ? null : route.targetCode());
        po.setExecuteStatus(transferResult.success() ? "SUCCESS" : "FAILED");
        po.setRetryCount(retryCount == null ? 0 : retryCount);
        po.setRequestSnapshotJson(transferJsonMapper.toJson(route));
        po.setResponseSnapshotJson(transferJsonMapper.toJson(transferResult.messages()));
        po.setDeliveredAt(LocalDateTime.now());
        transferDeliveryRecordRepository.insert(po);
    }

    @Override
    public long countByRouteId(Long routeId) {
        if (routeId == null) {
            return 0L;
        }
        return transferDeliveryRecordRepository.selectCount(
                Wrappers.lambdaQuery(TransferDeliveryRecordPO.class)
                        .eq(TransferDeliveryRecordPO::getRouteId, routeId)
        );
    }
}
