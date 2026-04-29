package com.yss.valset.analysis.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.analysis.domain.gateway.ParseQueueGateway;
import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.analysis.domain.model.ParseQueuePage;
import com.yss.valset.analysis.domain.model.ParseStatus;
import com.yss.valset.analysis.infrastructure.convertor.ParseQueueMapper;
import com.yss.valset.analysis.infrastructure.entity.ParseQueuePO;
import com.yss.valset.analysis.infrastructure.mapper.ParseQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis 支持的待解析任务网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class ParseQueueGatewayImpl implements ParseQueueGateway {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ParseQueueRepository transferParseQueueRepository;

    @Override
    public Optional<ParseQueue> findById(String queueId) {
        return Optional.ofNullable(transferParseQueueRepository.selectById(parseLong(queueId))).map(this::toDomain);
    }

    @Override
    public Optional<ParseQueue> findByBusinessKey(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(transferParseQueueRepository.selectOne(
                Wrappers.lambdaQuery(ParseQueuePO.class)
                        .eq(ParseQueuePO::getBusinessKey, businessKey.trim())
                        .last("limit 1")
        )).map(this::toDomain);
    }

    @Override
    public ParseQueuePage pageQueues(String transferId,
                                             String businessKey,
                                             String sourceCode,
                                             String routeId,
                                             String tagCode,
                                             String fileStatus,
                                             String deliveryStatus,
                                             String parseStatus,
                                             String triggerMode,
                                             Integer pageIndex,
                                             Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Page<ParseQueuePO> page = transferParseQueueRepository.selectPage(
                new Page<>(current, size),
                buildQuery(transferId, businessKey, sourceCode, routeId, tagCode, fileStatus, deliveryStatus, parseStatus, triggerMode)
                        .orderByDesc(ParseQueuePO::getCreatedAt)
                        .orderByDesc(ParseQueuePO::getQueueId)
        );
        List<ParseQueue> records = page.getRecords() == null ? List.of() : page.getRecords().stream().map(this::toDomain).toList();
        return new ParseQueuePage(records, page.getTotal(), (int) page.getCurrent() - 1, (int) page.getSize());
    }

    @Override
    public List<ParseQueue> listQueues(String transferId,
                                               String businessKey,
                                               String sourceCode,
                                               String routeId,
                                               String tagCode,
                                               String fileStatus,
                                               String deliveryStatus,
                                               String parseStatus,
                                               String triggerMode,
                                               Integer limit) {
        var query = buildQuery(transferId, businessKey, sourceCode, routeId, tagCode, fileStatus, deliveryStatus, parseStatus, triggerMode)
                .orderByDesc(ParseQueuePO::getCreatedAt)
                .orderByDesc(ParseQueuePO::getQueueId);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferParseQueueRepository.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ParseQueue> listPendingQueues(Integer limit) {
        var query = Wrappers.lambdaQuery(ParseQueuePO.class)
                .eq(ParseQueuePO::getParseStatus, ParseStatus.PENDING.name())
                .orderByAsc(ParseQueuePO::getCreatedAt)
                .orderByAsc(ParseQueuePO::getQueueId);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferParseQueueRepository.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean subscribeIfPending(String queueId, String subscribedBy, Instant subscribedAt) {
        Long id = parseLong(queueId);
        if (id == null) {
            return false;
        }
        LocalDateTime localDateTime = subscribedAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(subscribedAt, ZoneId.systemDefault());
        return transferParseQueueRepository.subscribeIfPending(id, subscribedBy, localDateTime, LocalDateTime.now()) > 0;
    }

    @Override
    public ParseQueue save(ParseQueue queue) {
        ParseQueuePO po = ParseQueueMapper.INSTANCE.toPO(queue);
        if (po.getQueueId() == null) {
            transferParseQueueRepository.insert(po);
        } else {
            transferParseQueueRepository.updateById(po);
        }
        return toDomain(po);
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ParseQueuePO> buildQuery(String transferId,
                                                                                                            String businessKey,
                                                                                                            String sourceCode,
                                                                                                            String routeId,
                                                                                                            String tagCode,
                                                                                                            String fileStatus,
                                                                                                            String deliveryStatus,
                                                                                                            String parseStatus,
                                                                                                            String triggerMode) {
        return Wrappers.lambdaQuery(ParseQueuePO.class)
                .eq(StringUtils.hasText(transferId), ParseQueuePO::getTransferId, transferId == null ? null : transferId.trim())
                .like(StringUtils.hasText(businessKey), ParseQueuePO::getBusinessKey, businessKey == null ? null : businessKey.trim())
                .like(StringUtils.hasText(sourceCode), ParseQueuePO::getSourceCode, sourceCode == null ? null : sourceCode.trim())
                .like(StringUtils.hasText(routeId), ParseQueuePO::getRouteId, routeId == null ? null : routeId.trim())
                .like(StringUtils.hasText(tagCode), ParseQueuePO::getTagCode, tagCode == null ? null : tagCode.trim())
                .like(StringUtils.hasText(fileStatus), ParseQueuePO::getFileStatus, fileStatus == null ? null : fileStatus.trim())
                .like(StringUtils.hasText(deliveryStatus), ParseQueuePO::getDeliveryStatus, deliveryStatus == null ? null : deliveryStatus.trim())
                .eq(StringUtils.hasText(parseStatus), ParseQueuePO::getParseStatus, parseStatus == null ? null : parseStatus.trim())
                .eq(StringUtils.hasText(triggerMode), ParseQueuePO::getTriggerMode, triggerMode == null ? null : triggerMode.trim());
    }

    private ParseQueue toDomain(ParseQueuePO po) {
        return ParseQueueMapper.INSTANCE.toDomain(po);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
