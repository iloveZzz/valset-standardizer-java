package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferObjectTag;

import java.util.List;

/**
 * 文件对象标签结果网关。
 */
public interface TransferObjectTagGateway {

    List<TransferObjectTag> listByTransferId(String transferId);

    List<TransferObjectTag> listByTransferIds(List<String> transferIds);

    List<TransferObjectTag> saveAll(List<TransferObjectTag> tags);

    void deleteByTransferId(String transferId);
}
