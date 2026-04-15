package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileIngestLogGateway;
import com.yss.subjectmatch.domain.model.SubjectMatchFileIngestLog;
import com.yss.subjectmatch.extract.repository.convertor.SubjectMatchFileIngestLogConvertor;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchFileIngestLogPO;
import com.yss.subjectmatch.extract.repository.mapper.SubjectMatchFileIngestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 文件接入日志网关实现。
 */
@Repository
@RequiredArgsConstructor
public class SubjectMatchFileIngestLogGatewayImpl implements SubjectMatchFileIngestLogGateway {

    private final SubjectMatchFileIngestLogRepository ingestLogRepository;
    private final SubjectMatchFileIngestLogConvertor ingestLogConvertor;

    @Override
    public Long save(SubjectMatchFileIngestLog ingestLog) {
        SubjectMatchFileIngestLogPO po = ingestLogConvertor.toPO(ingestLog);
        ingestLogRepository.insert(po);
        ingestLog.setIngestId(po.getIngestId());
        return po.getIngestId();
    }

    @Override
    public List<SubjectMatchFileIngestLog> findByFileId(Long fileId) {
        if (fileId == null) {
            return Collections.emptyList();
        }
        List<SubjectMatchFileIngestLogPO> poList = ingestLogRepository.selectList(
                Wrappers.lambdaQuery(SubjectMatchFileIngestLogPO.class)
                        .eq(SubjectMatchFileIngestLogPO::getFileId, fileId)
                        .orderByDesc(SubjectMatchFileIngestLogPO::getIngestTime)
        );
        if (poList == null || poList.isEmpty()) {
            return Collections.emptyList();
        }
        return poList.stream().map(ingestLogConvertor::toDomain).toList();
    }
}
