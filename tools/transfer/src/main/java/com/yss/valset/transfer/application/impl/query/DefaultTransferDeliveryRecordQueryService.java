package com.yss.valset.transfer.application.impl.query;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferDeliveryRecordViewDTO;
import com.yss.valset.transfer.application.service.TransferDeliveryRecordQueryService;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecordPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 默认文件投递结果查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferDeliveryRecordQueryService implements TransferDeliveryRecordQueryService {

    private final TransferDeliveryGateway transferDeliveryGateway;

    @Override
    public List<TransferDeliveryRecordViewDTO> listRecords(String routeId, String transferId, String targetCode, String executeStatus, Integer limit) {
        return transferDeliveryGateway.listRecords(routeId, transferId, targetCode, executeStatus, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public PageResult<TransferDeliveryRecordViewDTO> pageRecords(String routeId,
                                                                 String transferId,
                                                                 String targetCode,
                                                                 String executeStatus,
                                                                 Integer pageIndex,
                                                                 Integer pageSize) {
        TransferDeliveryRecordPage page = transferDeliveryGateway.pageRecords(routeId, transferId, targetCode, executeStatus, pageIndex, pageSize);
        List<TransferDeliveryRecordViewDTO> records = page.records() == null ? List.of() : page.records().stream().map(this::toView).toList();
        return PageResult.of(records, page.total(), page.pageSize(), pageIndex);
    }

    @Override
    public TransferDeliveryRecordViewDTO getRecord(String deliveryId) {
        return toView(transferDeliveryGateway.findById(deliveryId)
                .orElseThrow(() -> new IllegalStateException("未找到文件投递结果，deliveryId=" + deliveryId)));
    }

    private TransferDeliveryRecordViewDTO toView(TransferDeliveryRecord record) {
        return TransferDeliveryRecordViewDTO.builder()
                .deliveryId(record.deliveryId() == null ? null : String.valueOf(record.deliveryId()))
                .routeId(record.routeId() == null ? null : String.valueOf(record.routeId()))
                .transferId(record.transferId() == null ? null : String.valueOf(record.transferId()))
                .targetType(record.targetType())
                .targetCode(record.targetCode())
                .executeStatus(record.executeStatus())
                .executeStatusLabel(resolveExecuteStatusLabel(record.executeStatus()))
                .requestSnapshotJson(record.requestSnapshotJson())
                .responseSnapshotJson(record.responseSnapshotJson())
                .errorMessage(record.errorMessage())
                .deliveredAt(record.deliveredAt())
                .build();
    }

    private String resolveExecuteStatusLabel(String executeStatus) {
        if (executeStatus == null || executeStatus.isBlank()) {
            return "-";
        }
        return switch (executeStatus.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "SUCCEEDED", "DONE", "COMPLETED" -> "成功";
            case "FAILED", "FAIL", "ERROR" -> "失败";
            case "PENDING" -> "待处理";
            case "RUNNING", "PROCESSING" -> "执行中";
            case "RETRYING" -> "重试中";
            case "SKIPPED" -> "已跳过";
            default -> executeStatus;
        };
    }
}
