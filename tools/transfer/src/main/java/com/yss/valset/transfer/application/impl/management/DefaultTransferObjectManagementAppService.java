package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferObjectRetagCommand;
import com.yss.valset.transfer.application.command.TransferObjectRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferObjectRetagItemViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectRetagResponse;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverItemViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverResponse;
import com.yss.valset.transfer.application.service.TransferObjectManagementAppService;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认文件主对象管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferObjectManagementAppService implements TransferObjectManagementAppService {

    private static final int RETAG_PAGE_SIZE = 200;

    private final TransferObjectGateway transferObjectGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TransferProcessUseCase transferProcessUseCase;
    private final TransferTaggingUseCase transferTaggingUseCase;
    private final TransferTagGateway transferTagGateway;

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

    @Override
    public TransferObjectRetagResponse retag(TransferObjectRetagCommand command) {
        if (transferTagGateway.listEnabledTags().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前没有已激活的标签规则，无法重新打标");
        }
        List<TransferObject> transferObjects = loadMatchedTransferObjects(command);
        if (transferObjects.isEmpty()) {
            return TransferObjectRetagResponse.builder()
                    .requestedCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .matchedTagCount(0)
                    .items(List.of())
                    .build();
        }

        int successCount = 0;
        int failureCount = 0;
        int matchedTagCount = 0;
        List<TransferObjectRetagItemViewDTO> items = new ArrayList<>();

        for (TransferObject transferObject : transferObjects) {
            String transferId = trimToEmpty(transferObject.transferId());
            if (!StringUtils.hasText(transferId)) {
                failureCount++;
                items.add(buildRetagItem(null, false, 0, "文件主对象缺少主键，无法重新打标"));
                continue;
            }
            try {
                List<TransferObjectTag> tags = transferTaggingUseCase.retag(transferId, true);
                int tagCount = tags == null ? 0 : tags.size();
                matchedTagCount += tagCount;
                successCount++;
                items.add(buildRetagItem(transferId, true, tagCount, "重新打标成功，命中 " + tagCount + " 条标签"));
            } catch (RuntimeException exception) {
                failureCount++;
                items.add(buildRetagItem(transferId, false, 0, buildRetagFailureMessage(exception)));
            }
        }

        return TransferObjectRetagResponse.builder()
                .requestedCount(transferObjects.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .matchedTagCount(matchedTagCount)
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

    private TransferObjectRetagItemViewDTO buildRetagItem(String transferId,
                                                          boolean success,
                                                          int tagCount,
                                                          String message) {
        return TransferObjectRetagItemViewDTO.builder()
                .transferId(transferId)
                .success(success)
                .tagCount(tagCount)
                .message(message)
                .build();
    }

    private List<TransferObject> loadMatchedTransferObjects(TransferObjectRetagCommand command) {
        List<TransferObject> results = new ArrayList<>();
        int pageIndex = 0;
        while (true) {
            TransferObjectPage page = transferObjectGateway.pageObjects(
                    trimToNull(command == null ? null : command.getSourceId()),
                    trimToNull(command == null ? null : command.getSourceType()),
                    trimToNull(command == null ? null : command.getSourceCode()),
                    trimToNull(command == null ? null : command.getStatus()),
                    null,
                    trimToNull(command == null ? null : command.getMailId()),
                    trimToNull(command == null ? null : command.getFingerprint()),
                    trimToNull(command == null ? null : command.getRouteId()),
                    trimToNull(command == null ? null : command.getTagId()),
                    trimToNull(command == null ? null : command.getTagCode()),
                    trimToNull(command == null ? null : command.getTagValue()),
                    pageIndex,
                    RETAG_PAGE_SIZE
            );
            List<TransferObject> records = page == null || page.records() == null ? List.of() : page.records();
            if (records.isEmpty()) {
                break;
            }
            results.addAll(records);
            long total = page.total();
            if (results.size() >= total) {
                break;
            }
            pageIndex++;
        }
        return results;
    }

    private String buildFailureMessage(RuntimeException exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return "重新投递失败";
        }
        return "重新投递失败：" + exception.getMessage();
    }

    private String buildRetagFailureMessage(RuntimeException exception) {
        if (exception == null || !StringUtils.hasText(exception.getMessage())) {
            return "重新打标失败";
        }
        return "重新打标失败：" + exception.getMessage();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
