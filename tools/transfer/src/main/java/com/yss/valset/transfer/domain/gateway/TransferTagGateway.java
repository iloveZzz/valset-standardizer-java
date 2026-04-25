package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.domain.model.TransferTagPage;

import java.util.Optional;

/**
 * 标签配置网关。
 */
public interface TransferTagGateway {

    Optional<TransferTagDefinition> findById(String tagId);

    Optional<TransferTagDefinition> findByTagCode(String tagCode);

    TransferTagPage pageTags(String tagCode, String matchStrategy, Boolean enabled, Integer pageIndex, Integer pageSize);

    java.util.List<TransferTagDefinition> listEnabledTags();

    TransferTagDefinition save(TransferTagDefinition definition);

    void deleteById(String tagId);
}
