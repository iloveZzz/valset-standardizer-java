package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.domain.model.SubjectMatchFileSourceChannel;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStatus;
import com.yss.subjectmatch.extract.repository.convertor.SubjectMatchFileInfoConvertor;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchFileInfoPO;
import com.yss.subjectmatch.extract.repository.mapper.SubjectMatchFileInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件主数据网关实现。
 */
@Repository
@RequiredArgsConstructor
public class SubjectMatchFileInfoGatewayImpl implements SubjectMatchFileInfoGateway {

    private final SubjectMatchFileInfoRepository fileInfoRepository;
    private final SubjectMatchFileInfoConvertor fileInfoConvertor;

    @Override
    public Long save(SubjectMatchFileInfo fileInfo) {
        SubjectMatchFileInfoPO po = fileInfoConvertor.toPO(fileInfo);
        fileInfoRepository.insert(po);
        fileInfo.setFileId(po.getFileId());
        return po.getFileId();
    }

    @Override
    public SubjectMatchFileInfo findById(Long fileId) {
        SubjectMatchFileInfoPO po = fileInfoRepository.selectById(fileId);
        return po == null ? null : fileInfoConvertor.toDomain(po);
    }

    @Override
    public SubjectMatchFileInfo findByFingerprint(String fileFingerprint) {
        if (fileFingerprint == null || fileFingerprint.isBlank()) {
            return null;
        }
        List<SubjectMatchFileInfoPO> poList = fileInfoRepository.selectList(
                Wrappers.lambdaQuery(SubjectMatchFileInfoPO.class)
                        .eq(SubjectMatchFileInfoPO::getFileFingerprint, fileFingerprint)
                        .orderByDesc(SubjectMatchFileInfoPO::getFileId)
        );
        SubjectMatchFileInfoPO po = poList == null || poList.isEmpty() ? null : poList.get(0);
        return po == null ? null : fileInfoConvertor.toDomain(po);
    }

    @Override
    public List<SubjectMatchFileInfo> search(SubjectMatchFileSourceChannel sourceChannel,
                                             SubjectMatchFileStatus fileStatus,
                                             String fileFingerprint) {
        List<SubjectMatchFileInfoPO> poList = fileInfoRepository.selectList(
                Wrappers.lambdaQuery(SubjectMatchFileInfoPO.class)
                        .eq(sourceChannel != null, SubjectMatchFileInfoPO::getSourceChannel, sourceChannel.name())
                        .eq(fileStatus != null, SubjectMatchFileInfoPO::getFileStatus, fileStatus.name())
                        .eq(fileFingerprint != null && !fileFingerprint.isBlank(), SubjectMatchFileInfoPO::getFileFingerprint, fileFingerprint)
                        .orderByDesc(SubjectMatchFileInfoPO::getFileId)
        );
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        return poList.stream().map(fileInfoConvertor::toDomain).toList();
    }

    @Override
    public void updateStatus(Long fileId,
                             SubjectMatchFileStatus fileStatus,
                             Long lastTaskId,
                             LocalDateTime lastProcessedAt,
                             String errorMessage) {
        fileInfoRepository.update(
                null,
                Wrappers.lambdaUpdate(SubjectMatchFileInfoPO.class)
                        .eq(SubjectMatchFileInfoPO::getFileId, fileId)
                        .set(fileStatus != null, SubjectMatchFileInfoPO::getFileStatus, fileStatus == null ? null : fileStatus.name())
                        .set(lastTaskId != null, SubjectMatchFileInfoPO::getLastTaskId, lastTaskId)
                        .set(lastProcessedAt != null, SubjectMatchFileInfoPO::getLastProcessedAt, lastProcessedAt)
                        .set(errorMessage != null, SubjectMatchFileInfoPO::getErrorMessage, errorMessage)
        );
    }
}
