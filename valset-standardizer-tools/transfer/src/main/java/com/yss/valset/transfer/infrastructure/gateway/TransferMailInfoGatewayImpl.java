package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferMailInfoGateway;
import com.yss.valset.transfer.domain.model.TransferMailInfo;
import com.yss.valset.transfer.infrastructure.convertor.TransferMailInfoMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferMailInfoPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferMailInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis 邮件信息网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferMailInfoGatewayImpl implements TransferMailInfoGateway {

    private final TransferMailInfoRepository transferMailInfoRepository;
    private final TransferMailInfoMapper transferMailInfoMapper;

    @Override
    public Optional<TransferMailInfo> findByTransferId(String transferId) {
        if (transferId == null || transferId.isBlank()) {
            return Optional.empty();
        }
        TransferMailInfoPO po = transferMailInfoRepository.selectById(transferId);
        return Optional.ofNullable(po).map(transferMailInfoMapper::toDomain);
    }

    @Override
    public List<TransferMailInfo> listByTransferIds(List<String> transferIds) {
        if (transferIds == null || transferIds.isEmpty()) {
            return Collections.emptyList();
        }
        return transferMailInfoRepository.selectList(
                Wrappers.lambdaQuery(TransferMailInfoPO.class)
                        .in(TransferMailInfoPO::getTransferId, transferIds)
        ).stream().map(transferMailInfoMapper::toDomain).toList();
    }

    @Override
    public TransferMailInfo save(TransferMailInfo mailInfo) {
        if (mailInfo == null || mailInfo.transferId() == null || mailInfo.transferId().isBlank() || !hasAnyMailField(mailInfo)) {
            return mailInfo;
        }
        TransferMailInfoPO po = transferMailInfoMapper.toPO(mailInfo);
        if (transferMailInfoRepository.selectById(mailInfo.transferId()) == null) {
            transferMailInfoRepository.insert(po);
        } else {
            transferMailInfoRepository.updateById(po);
        }
        return transferMailInfoMapper.toDomain(po);
    }

    @Override
    public void deleteByTransferId(String transferId) {
        if (transferId == null || transferId.isBlank()) {
            return;
        }
        transferMailInfoRepository.deleteById(transferId);
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
        return value != null && !value.isBlank();
    }
}
