package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.domain.gateway.MatchResultGateway;
import com.yss.valset.domain.model.ValsetMatchResult;
import com.yss.valset.extract.repository.mapper.ValsetMatchResultRepository;
import com.yss.valset.extract.repository.convertor.ValsetMatchResultConvertor;
import com.yss.valset.extract.repository.entity.ValsetMatchResultPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的比赛结果网关。
 */
@Repository
@RequiredArgsConstructor
public class MatchResultGatewayImpl implements MatchResultGateway {

    private final ValsetMatchResultRepository subjectMatchResultRepository;
    private final ValsetMatchResultConvertor subjectMatchResultConvertor;

    /**
     * 保留批量匹配结果。
     */
    @Override
    public void saveResults(Long taskId, Long fileId, List<ValsetMatchResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        List<ValsetMatchResultPO> poList = results.stream()
                .map(result -> subjectMatchResultConvertor.toPO(taskId, fileId, result))
                .collect(Collectors.toList());
        subjectMatchResultRepository.insertBatchSomeColumn(poList);
    }

    /**
     * 按文件标识查询匹配结果。
     */
    @Override
    public List<ValsetMatchResult> findByFileId(Long fileId) {
        List<ValsetMatchResultPO> poList = subjectMatchResultRepository.selectList(
                Wrappers.lambdaQuery(ValsetMatchResultPO.class)
                        .eq(ValsetMatchResultPO::getFileId, fileId)
                        .orderByDesc(ValsetMatchResultPO::getScore)
                        .orderByAsc(ValsetMatchResultPO::getExternalSubjectCode)
        );
        return poList.stream()
                .map(subjectMatchResultConvertor::toDomain)
                .collect(Collectors.toList());
    }
}
