package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectPage;

import java.util.List;
import java.util.Optional;

/**
 * 文件主对象网关。
 */
public interface TransferObjectGateway {

    Optional<TransferObject> findById(String transferId);

    Optional<TransferObject> findByFingerprint(String fingerprint);

    TransferObjectPage pageObjects(String sourceId,
                                   String sourceType,
                                   String sourceCode,
                                   String status,
                                   String mailId,
                                   String fingerprint,
                                   String routeId,
                                   String tagId,
                                   String tagCode,
                                   String tagValue,
                                   Integer pageIndex,
                                   Integer pageSize);

    TransferObjectAnalysis analyzeObjects(String sourceId,
                                          String sourceType,
                                          String sourceCode,
                                          String status,
                                          String mailId,
                                          String fingerprint,
                                          String routeId,
                                          String tagId,
                                          String tagCode,
                                          String tagValue);

    TransferObject save(TransferObject transferObject);
}
