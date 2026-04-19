package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.domain.gateway.ValsetFileIngestLogGateway;
import com.yss.valset.domain.model.ValsetFileIngestLog;
import com.yss.valset.extract.repository.convertor.ValsetFileIngestLogConvertor;
import com.yss.valset.extract.repository.entity.ValsetFileIngestLogPO;
import com.yss.valset.extract.repository.mapper.ValsetFileIngestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 文件接入日志网关实现。
 */
@Repository
@RequiredArgsConstructor
public class ValsetFileIngestLogGatewayImpl implements ValsetFileIngestLogGateway {

    private final ValsetFileIngestLogRepository ingestLogRepository;
    private final ValsetFileIngestLogConvertor ingestLogConvertor;

    @Override
    public Long save(ValsetFileIngestLog ingestLog) {
        ValsetFileIngestLogPO po = ingestLogConvertor.toPO(ingestLog);
        ingestLogRepository.insert(po);
        ingestLog.setIngestId(po.getIngestId());
        return po.getIngestId();
    }

    @Override
    public List<ValsetFileIngestLog> findByFileId(Long fileId) {
        if (fileId == null) {
            return Collections.emptyList();
        }
        List<ValsetFileIngestLogPO> poList = ingestLogRepository.selectList(
                Wrappers.lambdaQuery(ValsetFileIngestLogPO.class)
                        .eq(ValsetFileIngestLogPO::getFileId, fileId)
                        .orderByDesc(ValsetFileIngestLogPO::getIngestTime)
        );
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return poList.stream().map(ingestLogConvertor::toDomain).toList();
    }
}
