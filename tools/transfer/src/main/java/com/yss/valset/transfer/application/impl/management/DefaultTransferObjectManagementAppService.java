package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferObjectRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverItemViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverResponse;
import com.yss.valset.transfer.application.service.TransferObjectManagementAppService;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认文件主对象管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferObjectManagementAppService implements TransferObjectManagementAppService {

    private final TransferObjectGateway transferObjectGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TransferProcessUseCase transferProcessUseCase;

    @Override
    public TransferObjectRedeliverResponse redeliver(TransferObjectRedeliverCommand command) {
        List<String> transferIds = normalizeIds(command == null ? null : command.getTransferIds());
        if (transferIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要重新投递的文件主对象");
        }

        Set<String> deliveredTransferIds = transferDeliveryGateway.listRecordsByTransferIds(transferIds, "SUCCESS").stream()
                .map(record -> record.transferId() == null ? null : record.transferId().trim())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        List<TransferObjectRedeliverItemViewDTO> items = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        for (String transferId : transferIds) {
            TransferObject transferObject = transferObjectGateway.findById(transferId).orElse(null);
            if (transferObject == null) {
                failureCount++;
                items.add(buildItem(transferId, null, false, "未找到文件主对象，transferId=" + transferId));
                continue;
            }

            String normalizedTransferId = trimToEmpty(transferObject.transferId());
            if (deliveredTransferIds.contains(normalizedTransferId)) {
                skippedCount++;
                items.add(buildItem(normalizedTransferId, transferObject.routeId(), false, "已投递，无需重新投递"));
                continue;
            }

            if (!StringUtils.hasText(transferObject.routeId())) {
                failureCount++;
                items.add(buildItem(normalizedTransferId, null, false, "文件主对象缺少路由主键，无法重新投递"));
                continue;
            }

            try {
                transferProcessUseCase.deliver(transferObject.routeId(), normalizedTransferId);
                successCount++;
                items.add(buildItem(normalizedTransferId, transferObject.routeId(), true, "重新投递成功"));
            } catch (RuntimeException exception) {
                failureCount++;
                items.add(buildItem(normalizedTransferId, transferObject.routeId(), false, buildFailureMessage(exception)));
            }
        }

        return TransferObjectRedeliverResponse.builder()
                .requestedCount(transferIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .skippedCount(skippedCount)
                .items(items)
                .build();
    }

    private List<String> normalizeIds(List<String> transferIds) {
        if (transferIds == null || transferIds.isEmpty()) {
            return List.of();
        }
        return transferIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private TransferObjectRedeliverItemViewDTO buildItem(String transferId,
                                                         String routeId,
                                                         boolean success,
                                                         String message) {
        return TransferObjectRedeliverItemViewDTO.builder()
                .transferId(transferId)
                .routeId(routeId)
                .success(success)
                .message(message)
                .build();
    }

    private String buildFailureMessage(RuntimeException exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return "重新投递失败";
        }
        return "重新投递失败：" + exception.getMessage();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
