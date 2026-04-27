package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferMailInfo;

import java.util.List;
import java.util.Optional;

/**
 * 邮件信息网关。
 */
public interface TransferMailInfoGateway {

    Optional<TransferMailInfo> findByTransferId(String transferId);

    List<TransferMailInfo> listByTransferIds(List<String> transferIds);

    TransferMailInfo save(TransferMailInfo mailInfo);

    void deleteByTransferId(String transferId);
}
