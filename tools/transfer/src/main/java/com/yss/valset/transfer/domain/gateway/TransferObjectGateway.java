package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferObject;

import java.util.Optional;

/**
 * 文件主对象网关。
 */
public interface TransferObjectGateway {

    Optional<TransferObject> findById(Long transferId);

    Optional<TransferObject> findByFingerprint(String fingerprint);

    TransferObject save(TransferObject transferObject);
}
