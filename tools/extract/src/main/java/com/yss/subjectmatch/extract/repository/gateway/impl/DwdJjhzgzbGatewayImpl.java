package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.yss.subjectmatch.domain.gateway.DwdJjhzgzbGateway;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.repository.entity.TrDwdJjhzgzbPO;
import com.yss.subjectmatch.extract.repository.mapper.TrDwdJjhzgzbRepository;
import com.yss.subjectmatch.extract.support.JjhzgzbStandardizationSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * t_tr_jjhzgzb 标准化落库网关实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DwdJjhzgzbGatewayImpl implements DwdJjhzgzbGateway {

    private final TrDwdJjhzgzbRepository repository;

    @Override
    public void saveStandardizedJjhzgzb(Long taskId, Long fileId, String sourceTp, String sourceSign, ParsedValuationData standardizedValuationData) {
        List<TrDwdJjhzgzbPO> rows = JjhzgzbStandardizationSupport.buildRows(standardizedValuationData, sourceTp, sourceSign);
        if (rows.isEmpty()) {
            log.info("t_tr_jjhzgzb 标准化结果为空，taskId={}, fileId={}, sourceTp={}", taskId, fileId, sourceTp);
            return;
        }
        repository.insertBatchSomeColumn(rows);
        TrDwdJjhzgzbPO firstRow = rows.get(0);
        log.info("t_tr_jjhzgzb 标准化落地完成，taskId={}, fileId={}, sourceTp={}, rowCount={}, firstSubjectCd={}, firstBizDate={}",
                taskId,
                fileId,
                sourceTp,
                rows.size(),
                firstRow.getSubjectCd(),
                firstRow.getBizDate());
    }
}
