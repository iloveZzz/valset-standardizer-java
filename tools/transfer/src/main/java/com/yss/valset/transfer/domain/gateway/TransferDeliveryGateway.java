package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecordPage;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;

import java.util.List;
import java.util.Optional;

/**
 * 投递结果网关。
 */
public interface TransferDeliveryGateway {

    TransferDeliveryRecord recordResult(String routeId, String transferId, TransferResult transferResult);

    TransferDeliveryRecord recordResult(String routeId, String transferId, TransferResult transferResult, Integer retryCount);

    long countByRouteId(String routeId);

    Optional<TransferDeliveryRecord> findById(String deliveryId);

    List<TransferDeliveryRecord> listRecords(String routeId, String transferId, String targetCode, String executeStatus, Integer limit);

    List<TransferDeliveryRecord> listRecordsByTransferIds(List<String> transferIds, String executeStatus);

    TransferDeliveryRecordPage pageRecords(String routeId, String transferId, String targetCode, String executeStatus, Integer pageIndex, Integer pageSize);
}
