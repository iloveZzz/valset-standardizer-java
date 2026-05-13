package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferTarget;

import java.util.List;
import java.util.Optional;

/**
 * 投递目标网关。
 */
public interface TransferTargetGateway {

    Optional<TransferTarget> findById(Long targetId);

    Optional<TransferTarget> findByTargetCode(String targetCode);

    List<TransferTarget> listEnabledTargets();

    List<TransferTarget> listTargets(String targetType, String targetCode, Boolean enabled, Integer limit);

    TransferTarget save(TransferTarget transferTarget);

    void deleteById(Long targetId);
}
