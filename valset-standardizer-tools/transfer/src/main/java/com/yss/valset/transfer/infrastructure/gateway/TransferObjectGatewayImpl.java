package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.application.impl.query.DefaultTransferObjectQueryService;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferMailInfoGateway;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectExtensionCount;
import com.yss.valset.transfer.domain.model.TransferObjectMailFolderCount;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectSourceAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectStatusCount;
import com.yss.valset.transfer.domain.model.TransferObjectSizeAnalysis;
import com.yss.valset.transfer.domain.model.TransferMailInfo;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.convertor.TransferObjectMapper;
import com.yss.valset.transfer.infrastructure.dto.MailInboxGroupDTO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectMybatisMapper;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的文件主对象网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferObjectGatewayImpl implements TransferObjectGateway {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TransferObjectRepository transferObjectRepository;
    private final TransferMailInfoGateway transferMailInfoGateway;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TransferJsonMapper transferJsonMapper;
    private final TransferObjectMapper transferObjectMapper;
    private final TransferObjectMybatisMapper transferObjectMybatisMapper;
    private final DatabaseDialectSupport databaseDialectSupport;

    @Override
    public Optional<TransferObject> findById(String transferId) {
        TransferObjectPO po = transferObjectRepository.selectById(parseLong(transferId));
        return Optional.ofNullable(po).map(this::toDomain).map(this::hydrateMailInfo);
    }

    @Override
    public Optional<TransferObject> findByFingerprint(String fingerprint) {
        TransferObjectPO po = transferObjectRepository.selectOne(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(TransferObjectPO::getFingerprint, fingerprint)
                        .last(databaseDialectSupport.limitClause(1))
        );
        return Optional.ofNullable(po).map(this::toDomain).map(this::hydrateMailInfo);
    }

    @Override
    public TransferObjectPage pageObjects(String sourceId,
                                          String sourceType,
                                          String sourceCode,
                                          String originalName,
                                          String status,
                                          String deliveryStatus,
                                          String mailId,
                                          String fingerprint,
                                          String routeId,
                                          String tagId,
                                          String tagCode,
                                          String tagValue,
                                          String taskDate,
                                          Integer pageIndex,
                                          Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
        LocalDateTime taskStart = resolveTaskStart(taskDate);
        LocalDateTime taskEnd = resolveTaskEnd(taskDate);
        String mailFilterSql = buildMailFilterSql(mailId);
        String tagFilterSql = buildTagFilterSql(tagId, tagCode, tagValue);
        String deliveryFilterSql = buildDeliveryFilterSql(deliveryStatus);
        Page<TransferObjectPO> page = transferObjectRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(sourceIdValue != null, TransferObjectPO::getSourceId, sourceIdValue)
                        .eq(sourceType != null && !sourceType.trim().isEmpty(), TransferObjectPO::getSourceType, sourceType)
                        .eq(sourceCode != null && !sourceCode.trim().isEmpty(), TransferObjectPO::getSourceCode, sourceCode)
                        .like(originalName != null && !originalName.trim().isEmpty(), TransferObjectPO::getOriginalName, originalName)
                        .eq(status != null && !status.trim().isEmpty(), TransferObjectPO::getStatus, status)
                        .ge(taskStart != null, TransferObjectPO::getReceivedAt, taskStart)
                        .lt(taskEnd != null, TransferObjectPO::getReceivedAt, taskEnd)
                        .inSql("DELIVERED".equalsIgnoreCase(deliveryStatus) && deliveryFilterSql != null, TransferObjectPO::getTransferId, deliveryFilterSql)
                        .notInSql("UNDELIVERED".equalsIgnoreCase(deliveryStatus) && deliveryFilterSql != null, TransferObjectPO::getTransferId, deliveryFilterSql)
                        .inSql(mailFilterSql != null, TransferObjectPO::getTransferId, mailFilterSql)
                        .eq(fingerprint != null && !fingerprint.trim().isEmpty(), TransferObjectPO::getFingerprint, fingerprint)
                        .eq(routeIdValue != null, TransferObjectPO::getRouteId, routeIdValue)
                        .inSql(tagFilterSql != null, TransferObjectPO::getTransferId, tagFilterSql)
                        .orderByDesc(TransferObjectPO::getReceivedAt)
                        .orderByDesc(TransferObjectPO::getTransferId)
        );
        List<TransferObject> records = page.getRecords() == null ? Collections.emptyList() : page.getRecords().stream()
                .map(this::toDomain)
                .map(this::hydrateMailInfo)
                .collect(java.util.stream.Collectors.toList());
        return new TransferObjectPage(records, page.getTotal(), page.getCurrent() - 1, page.getSize());
    }

    @Override
    public List<TransferObject> listEmailInboxObjects(String sourceCode, String mailId) {
        String mailFilterSql = buildMailFilterSql(mailId);
        List<TransferObject> objects = transferObjectRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectPO.class)
                                .eq(TransferObjectPO::getSourceType, "EMAIL")
                                .eq(sourceCode != null && !sourceCode.trim().isEmpty(), TransferObjectPO::getSourceCode, sourceCode)
                                .inSql(mailFilterSql != null, TransferObjectPO::getTransferId, mailFilterSql)
                                .orderByDesc(TransferObjectPO::getReceivedAt)
                                .orderByDesc(TransferObjectPO::getTransferId)
                )
                .stream()
                .map(this::toDomain)
                .collect(java.util.stream.Collectors.toList());
        if (objects.isEmpty()) {
            return objects;
        }
        List<String> transferIds = objects.stream()
                .map(TransferObject::transferId)
                .filter(this::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return objects;
        }
        Map<String, TransferMailInfo> mailInfoMap = transferMailInfoGateway.listByTransferIds(transferIds).stream()
                .collect(Collectors.toMap(
                        TransferMailInfo::transferId,
                        mailInfo -> mailInfo,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (mailInfoMap.isEmpty()) {
            return objects;
        }
        return objects.stream()
                .map(object -> {
                    TransferMailInfo mailInfo = mailInfoMap.get(object.transferId());
                    return mailInfo == null ? object : object.withMailInfo(mailInfo);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<TransferObject> listParseQueueCandidates(String sourceId,
                                                        String sourceCode,
                                                        String routeId,
                                                        String status,
                                                        String deliveryStatus,
                                                        Integer limit) {
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
        String deliveryFilterSql = buildDeliveryFilterSql(deliveryStatus);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TransferObjectPO> query = Wrappers.lambdaQuery(TransferObjectPO.class)
                .eq(sourceIdValue != null, TransferObjectPO::getSourceId, sourceIdValue)
                .eq(sourceCode != null && !sourceCode.trim().isEmpty(), TransferObjectPO::getSourceCode, sourceCode)
                .eq(status != null && !status.trim().isEmpty(), TransferObjectPO::getStatus, status)
                .eq(routeIdValue != null, TransferObjectPO::getRouteId, routeIdValue)
                .inSql("DELIVERED".equalsIgnoreCase(deliveryStatus) && deliveryFilterSql != null, TransferObjectPO::getTransferId, deliveryFilterSql)
                .notInSql("UNDELIVERED".equalsIgnoreCase(deliveryStatus) && deliveryFilterSql != null, TransferObjectPO::getTransferId, deliveryFilterSql)
                .orderByDesc(TransferObjectPO::getReceivedAt)
                .orderByDesc(TransferObjectPO::getTransferId);
        if (limit != null && limit > 0) {
            query.last(databaseDialectSupport.limitClause(limit));
        }
        return transferObjectRepository.selectList(query)
                .stream()
                .map(this::toDomain)
                .map(this::hydrateMailInfo)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public TransferObjectAnalysis analyzeObjects(String sourceId,
                                                 String sourceType,
                                                 String sourceCode,
                                                 String originalName,
                                                 String status,
                                                 String deliveryStatus,
                                                 String mailId,
                                                 String fingerprint,
                                          String routeId,
                                          String tagId,
                                          String tagCode,
                                          String tagValue,
                                          String taskDate) {
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
        LocalDateTime taskStart = resolveTaskStart(taskDate);
        LocalDateTime taskEnd = resolveTaskEnd(taskDate);
        String mailFilterSql = buildMailFilterSql(mailId);
        String tagFilterSql = buildTagFilterSql(tagId, tagCode, tagValue);
        String deliveryFilterSql = buildDeliveryFilterSql(deliveryStatus);
        List<TransferObject> objects = transferObjectRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(sourceIdValue != null, TransferObjectPO::getSourceId, sourceIdValue)
                        .eq(sourceType != null && !sourceType.trim().isEmpty(), TransferObjectPO::getSourceType, sourceType)
                        .eq(sourceCode != null && !sourceCode.trim().isEmpty(), TransferObjectPO::getSourceCode, sourceCode)
                        .like(originalName != null && !originalName.trim().isEmpty(), TransferObjectPO::getOriginalName, originalName)
                        .eq(status != null && !status.trim().isEmpty(), TransferObjectPO::getStatus, status)
                                .ge(taskStart != null, TransferObjectPO::getReceivedAt, taskStart)
                                .lt(taskEnd != null, TransferObjectPO::getReceivedAt, taskEnd)
                                .inSql("DELIVERED".equalsIgnoreCase(deliveryStatus) && deliveryFilterSql != null, TransferObjectPO::getTransferId, deliveryFilterSql)
                                .notInSql("UNDELIVERED".equalsIgnoreCase(deliveryStatus) && deliveryFilterSql != null, TransferObjectPO::getTransferId, deliveryFilterSql)
                                .inSql(mailFilterSql != null, TransferObjectPO::getTransferId, mailFilterSql)
                                .eq(fingerprint != null && !fingerprint.trim().isEmpty(), TransferObjectPO::getFingerprint, fingerprint)
                                .eq(routeIdValue != null, TransferObjectPO::getRouteId, routeIdValue)
                                .inSql(tagFilterSql != null, TransferObjectPO::getTransferId, tagFilterSql)
                                .orderByDesc(TransferObjectPO::getReceivedAt)
                                .orderByDesc(TransferObjectPO::getTransferId)
                )
                .stream()
                .map(this::toDomain)
                .map(this::hydrateMailInfo)
                .collect(java.util.stream.Collectors.toList());
        long taggedCount = countTaggedObjects(objects);
        long untaggedCount = Math.max(0L, objects.size() - taggedCount);
        Map<String, List<TransferObject>> sourceGroups = objects.stream()
                .collect(Collectors.groupingBy(
                        item -> normalizeSourceTypeKey(item.sourceType()),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<TransferObjectSourceAnalysis> sourceAnalyses = sourceGroups.entrySet().stream()
                .map(entry -> {
                    List<TransferObject> sourceObjects = entry.getValue();
                    Map<String, Long> statusCountMap = sourceObjects.stream()
                            .collect(Collectors.groupingBy(
                                    item -> normalizeStatusKey(item.status() == null ? null : item.status().name()),
                                    Collectors.counting()
                            ));
                    Map<String, Long> mailFolderCountMap = sourceObjects.stream()
                            .collect(Collectors.groupingBy(
                                    item -> normalizeMailFolderKey(item.mailFolder()),
                                    java.util.LinkedHashMap::new,
                                    Collectors.counting()
                            ));
                    List<TransferObjectStatusCount> statusCounts = orderStatusCounts(statusCountMap);
                    List<TransferObjectMailFolderCount> mailFolderCounts = orderMailFolderCounts(mailFolderCountMap);
                    Set<String> deliveredTransferIds = loadDeliveredTransferIds(sourceObjects);
                    long undeliveredCount = sourceObjects.stream()
                            .map(TransferObject::transferId)
                            .filter(java.util.Objects::nonNull)
                            .filter(transferId -> !deliveredTransferIds.contains(transferId))
                            .count();
                    return new TransferObjectSourceAnalysis(
                            entry.getKey(),
                            (long) sourceObjects.size(),
                            statusCounts,
                            mailFolderCounts,
                            undeliveredCount
                    );
                })
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.totalCount(), left.totalCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.sourceType()).compareToIgnoreCase(String.valueOf(right.sourceType()));
                })
                .collect(java.util.stream.Collectors.toList());
        return new TransferObjectAnalysis(
                (long) objects.size(),
                taggedCount,
                untaggedCount,
                sourceAnalyses,
                buildSizeAnalysis(objects)
        );
    }

    @Override
    public TransferObject save(TransferObject transferObject) {
        TransferObjectPO po = toPO(transferObject);
        if (po.getTransferId() == null) {
            transferObjectRepository.insert(po);
        } else {
            transferObjectRepository.updateById(po);
        }
        saveMailInfo(resolveTransferId(po, transferObject), transferObject);
        return hydrateMailInfo(toDomain(po));
    }

    @Override
    public List<DefaultTransferObjectQueryService.InboxMailGroup> loadMailInboxGroups(String sourceCode, String mailId, String deliveryStatus, Integer offset, Integer limit) {
        List<MailInboxGroupDTO> dtoList = transferObjectMybatisMapper.loadMailInboxGroups(sourceCode, mailId, deliveryStatus, offset, limit);
        if (dtoList.isEmpty()) {
            return java.util.Arrays.asList();
        }
        Map<String, List<MailInboxGroupDTO>> groupedMap = dtoList.stream()
                .collect(Collectors.groupingBy(
                        dto -> normalizeMailGroupKey(dto.getMailKey(), dto.getMailId(), dto.getTransferId()),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<DefaultTransferObjectQueryService.InboxMailGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<MailInboxGroupDTO>> entry : groupedMap.entrySet()) {
            String mailKey = entry.getKey();
            List<MailInboxGroupDTO> items = entry.getValue();

            if (items.isEmpty()) {
                continue;
            }
            MailInboxGroupDTO representativeDto = items.stream()
                    .filter(dto -> dto.getRowNum() != null && dto.getRowNum() == 1)
                    .findFirst()
                    .orElse(items.get(0));
            TransferObject representative = toDomainFromDto(representativeDto);
            List<TransferObject> attachments = items.stream()
                    .map(this::toDomainFromDto)
                    .collect(java.util.stream.Collectors.toList());
            List<String> transferIds = items.stream()
                    .map(MailInboxGroupDTO::getTransferId)
                    .filter(this::hasText)
                    .collect(java.util.stream.Collectors.toList());
            boolean delivered = items.stream().allMatch(dto -> dto.getDelivered() != null && dto.getDelivered() == 1);
            boolean tagged = items.stream().anyMatch(dto -> dto.getTagged() != null && dto.getTagged() == 1);

            groups.add(new DefaultTransferObjectQueryService.InboxMailGroup(
                    mailKey,
                    representative,
                    attachments,
                    transferIds,
                    null, // mailInfo will be hydrated later
                    delivered,
                    tagged
            ));
        }

        return groups;
    }

    @Override
    public long countMailInboxGroups(String sourceCode, String mailId, String deliveryStatus) {
        return transferObjectMybatisMapper.countMailInboxGroups(sourceCode, mailId, deliveryStatus);
    }

    private TransferObjectPO toPO(TransferObject transferObject) {
        return transferObjectMapper.toPO(transferObject, transferJsonMapper);
    }

    private TransferObject toDomain(TransferObjectPO po) {
        return transferObjectMapper.toDomain(po, transferJsonMapper);
    }

    private TransferObject hydrateMailInfo(TransferObject transferObject) {
        if (transferObject == null || transferObject.transferId() == null) {
            return transferObject;
        }
        return transferMailInfoGateway.findByTransferId(transferObject.transferId())
                .map(transferObject::withMailInfo)
                .orElse(transferObject);
    }

    private void saveMailInfo(String transferId, TransferObject transferObject) {
        if (transferObject == null || transferId == null || transferId.trim().isEmpty()) {
            return;
        }
        TransferMailInfo mailInfo = new TransferMailInfo(
                transferId,
                transferObject.mailId(),
                transferObject.mailFrom(),
                transferObject.mailTo(),
                transferObject.mailCc(),
                transferObject.mailBcc(),
                transferObject.mailSubject(),
                transferObject.mailBody(),
                transferObject.mailProtocol(),
                transferObject.mailFolder()
        );
        if (!hasAnyMailField(mailInfo)) {
            transferMailInfoGateway.deleteByTransferId(transferId);
            return;
        }
        transferMailInfoGateway.save(mailInfo);
    }

    private String resolveTransferId(TransferObjectPO po, TransferObject transferObject) {
        if (po != null && po.getTransferId() != null && !po.getTransferId().trim().isEmpty()) {
            return po.getTransferId();
        }
        return transferObject == null ? null : transferObject.transferId();
    }

    private boolean hasAnyMailField(TransferMailInfo mailInfo) {
        return hasText(mailInfo.mailId())
                || hasText(mailInfo.mailFrom())
                || hasText(mailInfo.mailTo())
                || hasText(mailInfo.mailCc())
                || hasText(mailInfo.mailBcc())
                || hasText(mailInfo.mailSubject())
                || hasText(mailInfo.mailBody())
                || hasText(mailInfo.mailProtocol())
                || hasText(mailInfo.mailFolder());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildTagFilterSql(String tagId, String tagCode, String tagValue) {
        StringBuilder sql = new StringBuilder("select transfer_id from t_transfer_object_tag where 1 = 1");
        boolean hasFilter = false;
        if (tagId != null && !tagId.trim().isEmpty()) {
            sql.append(" and tag_id = '").append(escapeSql(tagId.trim())).append("'");
            hasFilter = true;
        }
        if (tagCode != null && !tagCode.trim().isEmpty()) {
            sql.append(" and tag_code = '").append(escapeSql(tagCode.trim())).append("'");
            hasFilter = true;
        }
        if (tagValue != null && !tagValue.trim().isEmpty()) {
            sql.append(" and tag_value = '").append(escapeSql(tagValue.trim())).append("'");
            hasFilter = true;
        }
        return hasFilter ? sql.toString() : null;
    }

    private String buildDeliveryFilterSql(String deliveryStatus) {
        if (!"DELIVERED".equalsIgnoreCase(deliveryStatus) && !"UNDELIVERED".equalsIgnoreCase(deliveryStatus)) {
            return null;
        }
        return "select transfer_id from t_transfer_delivery_record where execute_status = 'SUCCESS'";
    }

    private String buildMailFilterSql(String mailId) {
        if (mailId == null || mailId.trim().isEmpty()) {
            return null;
        }
        return "select transfer_id from t_transfer_mail_info where mail_id = '" + escapeSql(mailId.trim()) + "'";
    }

    private Set<String> loadDeliveredTransferIds(List<TransferObject> objects) {
        List<String> transferIds = objects == null ? java.util.Arrays.asList() : objects.stream()
                .map(TransferObject::transferId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return new java.util.LinkedHashSet<>(java.util.Arrays.asList());
        }
        return transferDeliveryGateway.listRecordsByTransferIds(transferIds, "SUCCESS").stream()
                .map(TransferDeliveryRecord::transferId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private LocalDateTime resolveTaskStart(String taskDate) {
        LocalDate taskDay = resolveTaskDate(taskDate);
        return taskDay == null ? null : taskDay.atStartOfDay();
    }

    private LocalDateTime resolveTaskEnd(String taskDate) {
        LocalDate taskDay = resolveTaskDate(taskDate);
        return taskDay == null ? null : taskDay.plusDays(1).atStartOfDay();
    }

    private LocalDate resolveTaskDate(String taskDate) {
        if (taskDate == null || taskDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(taskDate.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("不支持的任务日期: " + taskDate, exception);
        }
    }

    private List<TransferObjectStatusCount> orderStatusCounts(Map<String, Long> statusCountMap) {
        return java.util.Arrays.stream(TransferStatus.values())
                .map(status -> {
                    String key = normalizeStatusKey(status.name());
                    Long count = statusCountMap.get(key);
                    if (count == null) {
                        return null;
                    }
                    return new TransferObjectStatusCount(status.name(), count);
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<TransferObjectMailFolderCount> orderMailFolderCounts(Map<String, Long> mailFolderCountMap) {
        return mailFolderCountMap.entrySet().stream()
                .map(entry -> new TransferObjectMailFolderCount(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.count(), left.count());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.mailFolder()).compareToIgnoreCase(String.valueOf(right.mailFolder()));
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private TransferObjectSizeAnalysis buildSizeAnalysis(List<TransferObject> objects) {
        long totalSizeBytes = 0L;
        Map<String, Long> extensionCountMap = new java.util.LinkedHashMap<>();
        for (TransferObject object : objects) {
            if (object != null && object.sizeBytes() != null) {
                totalSizeBytes += object.sizeBytes();
            }
            String extensionKey = normalizeExtensionKey(object == null ? null : object.extension());
            extensionCountMap.merge(extensionKey, 1L, Long::sum);
        }
        List<TransferObjectExtensionCount> extensionCounts = extensionCountMap.entrySet().stream()
                .map(entry -> new TransferObjectExtensionCount(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.count(), left.count());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.extension()).compareToIgnoreCase(String.valueOf(right.extension()));
                })
                .collect(java.util.stream.Collectors.toList());
        return new TransferObjectSizeAnalysis((long) objects.size(), totalSizeBytes, extensionCounts);
    }

    private long countTaggedObjects(List<TransferObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return 0L;
        }
        List<String> transferIds = objects.stream()
                .map(TransferObject::transferId)
                .filter(org.springframework.util.StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return 0L;
        }
        return transferObjectTagGateway.listByTransferIds(transferIds).stream()
                .map(tag -> tag.transferId() == null ? null : tag.transferId().trim())
                .filter(org.springframework.util.StringUtils::hasText)
                .distinct()
                .count();
    }

    private String normalizeStatusKey(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return "-";
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private String normalizeSourceTypeKey(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        return text.isEmpty() ? "-" : text;
    }

    private String normalizeMailFolderKey(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        return text.isEmpty() ? "-" : text;
    }

    private String normalizeExtensionKey(String value) {
        if (value == null) {
            return "无后缀";
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return "无后缀";
        }
        return text.startsWith(".") ? text.toLowerCase(Locale.ROOT) : "." + text.toLowerCase(Locale.ROOT);
    }

    private String normalizeMailGroupKey(String mailKey, String mailId, String transferId) {
        if (hasText(mailKey)) {
            return mailKey.trim();
        }
        if (hasText(mailId)) {
            return mailId.trim();
        }
        return hasText(transferId) ? "transfer:" + transferId.trim() : "-";
    }

    private TransferObject toDomainFromDto(MailInboxGroupDTO dto) {
        if (dto == null) {
            return null;
        }

        TransferStatus status = null;
        if (hasText(dto.getStatus())) {
            try {
                status = TransferStatus.valueOf(dto.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        Instant receivedAt = null;
        if (dto.getReceivedAt() != null) {
            receivedAt = dto.getReceivedAt().atZone(ZoneId.systemDefault()).toInstant();
        }

        Instant storedAt = null;
        if (dto.getStoredAt() != null) {
            storedAt = dto.getStoredAt().atZone(ZoneId.systemDefault()).toInstant();
        }

        ProbeResult probeResult = parseProbeResultFromDto(dto);

        return new TransferObject(
                dto.getTransferId(),
                dto.getSourceId(),
                dto.getSourceType(),
                dto.getSourceCode(),
                dto.getOriginalName(),
                dto.getExtension(),
                dto.getMimeType(),
                dto.getSizeBytes(),
                dto.getFingerprint(),
                dto.getSourceRef(),
                dto.getMailId(),
                dto.getMailFrom(),
                dto.getMailTo(),
                dto.getMailCc(),
                dto.getMailBcc(),
                dto.getMailSubject(),
                dto.getMailBody(),
                dto.getMailProtocol(),
                dto.getMailFolder(),
                dto.getLocalTempPath(),
                status,
                receivedAt,
                storedAt,
                dto.getBusinessDate(),
                dto.getBusinessId(),
                dto.getReceiveDate(),
                dto.getRouteId(),
                dto.getErrorMessage(),
                probeResult,
                mergeFileMetaFromDto(dto),
                dto.getRealStoragePath()
        );
    }

    private ProbeResult parseProbeResultFromDto(MailInboxGroupDTO dto) {
        Map<String, Object> stored = transferJsonMapper.toMap(dto.getProbeResultJson());
        if (stored == null || stored.isEmpty()) {
            stored = transferJsonMapper.toMap(dto.getFileMetaJson());
        }
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        Object detectedRaw = stored.get("detected");
        Object detectedTypeRaw = stored.get("detectedType");
        Object attributesRaw = stored.get("attributes");
        if (detectedRaw == null && detectedTypeRaw == null && attributesRaw == null) {
            detectedRaw = stored.get(TransferConfigKeys.PROBE_DETECTED);
            detectedTypeRaw = stored.get(TransferConfigKeys.PROBE_DETECTED_TYPE);
            attributesRaw = stored.get(TransferConfigKeys.PROBE_ATTRIBUTES);
        }
        return new ProbeResult(
                detectedRaw == null || Boolean.parseBoolean(String.valueOf(detectedRaw)),
                detectedTypeRaw == null ? null : String.valueOf(detectedTypeRaw),
                attributesRaw instanceof Map<?, ?> ? safeMap(castMap((Map<?, ?>) attributesRaw)) : java.util.Collections.emptyMap()
        );
    }

    private Map<String, Object> mergeFileMetaFromDto(MailInboxGroupDTO dto) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Map<String, Object> storedMeta = transferJsonMapper.toMap(dto.getFileMetaJson());
        if (storedMeta != null) {
            meta.putAll(storedMeta);
        }
        meta.putIfAbsent(TransferConfigKeys.SOURCE_TYPE, dto.getSourceType());
        meta.putIfAbsent(TransferConfigKeys.SOURCE_CODE, dto.getSourceCode());
        meta.putIfAbsent("realStoragePath", dto.getRealStoragePath());
        return meta;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        if (source == null) {
            return java.util.Collections.emptyMap();
        }
        return source;
    }
}
