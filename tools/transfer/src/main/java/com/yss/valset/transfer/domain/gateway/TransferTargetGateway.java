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

    TransferTarget save(TransferTarget transferTarget);
}
