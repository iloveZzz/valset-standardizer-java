package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.extract.repository.convertor.ValsetFileInfoConvertor;
import com.yss.valset.extract.repository.entity.ValsetFileInfoPO;
import com.yss.valset.extract.repository.mapper.ValsetFileInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件主数据网关实现。
 */
@Repository
@RequiredArgsConstructor
public class ValsetFileInfoGatewayImpl implements ValsetFileInfoGateway {

    private final ValsetFileInfoRepository fileInfoRepository;
    private final ValsetFileInfoConvertor fileInfoConvertor;

    @Override
    public Long save(ValsetFileInfo fileInfo) {
        ValsetFileInfoPO po = fileInfoConvertor.toPO(fileInfo);
        fileInfoRepository.insert(po);
        fileInfo.setFileId(po.getFileId());
        return po.getFileId();
    }

    @Override
    public ValsetFileInfo findById(Long fileId) {
        ValsetFileInfoPO po = fileInfoRepository.selectById(fileId);
        return po == null ? null : fileInfoConvertor.toDomain(po);
    }

    @Override
    public ValsetFileInfo findByFingerprint(String fileFingerprint) {
        if (fileFingerprint == null || fileFingerprint.isBlank()) {
            return null;
        }
        List<ValsetFileInfoPO> poList = fileInfoRepository.selectList(
                Wrappers.lambdaQuery(ValsetFileInfoPO.class)
                        .eq(ValsetFileInfoPO::getFileFingerprint, fileFingerprint)
                        .orderByDesc(ValsetFileInfoPO::getFileId)
        );
        ValsetFileInfoPO po = poList == null || poList.isEmpty() ? null : poList.get(0);
        return po == null ? null : fileInfoConvertor.toDomain(po);
    }

    @Override
    public List<ValsetFileInfo> search(ValsetFileSourceChannel sourceChannel,
                                             ValsetFileStatus fileStatus,
                                             String fileFingerprint) {
        List<ValsetFileInfoPO> poList = fileInfoRepository.selectList(
                Wrappers.lambdaQuery(ValsetFileInfoPO.class)
                        .eq(sourceChannel != null, ValsetFileInfoPO::getSourceChannel, sourceChannel.name())
                        .eq(fileStatus != null, ValsetFileInfoPO::getFileStatus, fileStatus.name())
                        .eq(fileFingerprint != null && !fileFingerprint.isBlank(), ValsetFileInfoPO::getFileFingerprint, fileFingerprint)
                        .orderByDesc(ValsetFileInfoPO::getFileId)
        );
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        return poList.stream().map(fileInfoConvertor::toDomain).toList();
    }

    @Override
    public void updateStatus(Long fileId,
                             ValsetFileStatus fileStatus,
                             Long lastTaskId,
                             LocalDateTime lastProcessedAt,
                             String errorMessage) {
        fileInfoRepository.update(
                null,
                Wrappers.lambdaUpdate(ValsetFileInfoPO.class)
                        .eq(ValsetFileInfoPO::getFileId, fileId)
                        .set(fileStatus != null, ValsetFileInfoPO::getFileStatus, fileStatus == null ? null : fileStatus.name())
                        .set(lastTaskId != null, ValsetFileInfoPO::getLastTaskId, lastTaskId)
                        .set(lastProcessedAt != null, ValsetFileInfoPO::getLastProcessedAt, lastProcessedAt)
                        .set(errorMessage != null, ValsetFileInfoPO::getErrorMessage, errorMessage)
        );
    }
}
