package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.domain.model.TransferTagPage;
import com.yss.valset.transfer.infrastructure.convertor.TransferTagMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferTagPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis 支持的标签配置网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferTagGatewayImpl implements TransferTagGateway {

    private final TransferTagRepository transferTagRepository;
    private final TransferTagMapper transferTagMapper;

    @Override
    public Optional<TransferTagDefinition> findById(String tagId) {
        return Optional.ofNullable(transferTagRepository.selectById(parseLong(tagId))).map(this::toDomain);
    }

    @Override
    public Optional<TransferTagDefinition> findByTagCode(String tagCode) {
        TransferTagPO po = transferTagRepository.selectOne(
                Wrappers.lambdaQuery(TransferTagPO.class)
                        .eq(TransferTagPO::getTagCode, tagCode)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public TransferTagPage pageTags(String tagCode, String matchStrategy, Boolean enabled, Integer pageIndex, Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        Page<TransferTagPO> page = transferTagRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(TransferTagPO.class)
                        .like(tagCode != null && !tagCode.isBlank(), TransferTagPO::getTagCode, tagCode)
                        .eq(matchStrategy != null && !matchStrategy.isBlank(), TransferTagPO::getMatchStrategy, matchStrategy)
                        .eq(enabled != null, TransferTagPO::getEnabled, enabled)
                        .orderByAsc(TransferTagPO::getPriority)
                        .orderByAsc(TransferTagPO::getTagId)
        );
        List<TransferTagDefinition> records = page.getRecords() == null
                ? List.of()
                : page.getRecords().stream().map(this::toDomain).toList();
        return new TransferTagPage(records, page.getTotal(), page.getCurrent() - 1, page.getSize());
    }

    @Override
    public List<TransferTagDefinition> listEnabledTags() {
        return transferTagRepository.selectList(
                        Wrappers.lambdaQuery(TransferTagPO.class)
                                .eq(TransferTagPO::getEnabled, Boolean.TRUE)
                                .orderByAsc(TransferTagPO::getPriority)
                                .orderByAsc(TransferTagPO::getTagId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TransferTagDefinition save(TransferTagDefinition definition) {
        TransferTagPO po = transferTagMapper.toPO(definition);
        if (po.getTagId() == null) {
            transferTagRepository.insert(po);
        } else {
            transferTagRepository.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public void deleteById(String tagId) {
        Long id = parseLong(tagId);
        if (id != null) {
            transferTagRepository.deleteById(id);
        }
    }

    private TransferTagDefinition toDomain(TransferTagPO po) {
        return transferTagMapper.toDomain(po);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
