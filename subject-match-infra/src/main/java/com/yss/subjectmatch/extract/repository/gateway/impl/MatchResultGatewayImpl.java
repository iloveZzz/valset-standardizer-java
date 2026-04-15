package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.domain.gateway.MatchResultGateway;
import com.yss.subjectmatch.domain.model.SubjectMatchResult;
import com.yss.subjectmatch.extract.repository.mapper.SubjectMatchResultRepository;
import com.yss.subjectmatch.extract.repository.convertor.SubjectMatchResultConvertor;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchResultPO;
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

    private final SubjectMatchResultRepository subjectMatchResultRepository;
    private final SubjectMatchResultConvertor subjectMatchResultConvertor;

    /**
     * 保留批量匹配结果。
     */
    @Override
    public void saveResults(Long taskId, Long fileId, List<SubjectMatchResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        List<SubjectMatchResultPO> poList = results.stream()
                .map(result -> subjectMatchResultConvertor.toPO(taskId, fileId, result))
                .collect(Collectors.toList());
        subjectMatchResultRepository.insertBatchSomeColumn(poList);
    }

    /**
     * 按文件标识查询匹配结果。
     */
    @Override
    public List<SubjectMatchResult> findByFileId(Long fileId) {
        List<SubjectMatchResultPO> poList = subjectMatchResultRepository.selectList(
                Wrappers.lambdaQuery(SubjectMatchResultPO.class)
                        .eq(SubjectMatchResultPO::getFileId, fileId)
                        .orderByDesc(SubjectMatchResultPO::getScore)
                        .orderByAsc(SubjectMatchResultPO::getExternalSubjectCode)
        );
        return poList.stream()
                .map(subjectMatchResultConvertor::toDomain)
                .collect(Collectors.toList());
    }
}
