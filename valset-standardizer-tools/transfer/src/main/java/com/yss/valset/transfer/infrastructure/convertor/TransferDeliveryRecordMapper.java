package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.infrastructure.entity.TransferDeliveryRecordPO;
import org.mapstruct.Mapper;

/**
 * 文件投递结果映射器。
 */
@Mapper(componentModel = "spring")
public interface TransferDeliveryRecordMapper extends TransferMapstructSupport {

    default TransferDeliveryRecordPO toPO(TransferDeliveryRecord transferDeliveryRecord) {
        if (transferDeliveryRecord == null) {
            return null;
        }
        TransferDeliveryRecordPO po = new TransferDeliveryRecordPO();
        po.setDeliveryId(transferDeliveryRecord.deliveryId());
        po.setRouteId(transferDeliveryRecord.routeId());
        po.setTransferId(transferDeliveryRecord.transferId());
        po.setTargetType(transferDeliveryRecord.targetType());
        po.setTargetCode(transferDeliveryRecord.targetCode());
        po.setExecuteStatus(transferDeliveryRecord.executeStatus());
        po.setRetryCount(transferDeliveryRecord.retryCount());
        po.setRequestSnapshotJson(transferDeliveryRecord.requestSnapshotJson());
        po.setResponseSnapshotJson(transferDeliveryRecord.responseSnapshotJson());
        po.setErrorMessage(transferDeliveryRecord.errorMessage());
        po.setDeliveredAt(transferDeliveryRecord.deliveredAt());
        return po;
    }

    default TransferDeliveryRecord toDomain(TransferDeliveryRecordPO po) {
        if (po == null) {
            return null;
        }
        return new TransferDeliveryRecord(
                po.getDeliveryId(),
                po.getRouteId(),
                po.getTransferId(),
                po.getTargetType(),
                po.getTargetCode(),
                po.getExecuteStatus(),
                po.getRetryCount(),
                po.getRequestSnapshotJson(),
                po.getResponseSnapshotJson(),
                po.getErrorMessage(),
                po.getDeliveredAt()
        );
    }
}
