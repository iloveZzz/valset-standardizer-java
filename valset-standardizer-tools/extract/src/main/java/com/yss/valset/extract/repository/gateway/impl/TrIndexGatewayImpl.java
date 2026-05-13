package com.yss.valset.extract.repository.gateway.impl;

import com.yss.valset.domain.gateway.TrIndexGateway;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.TrIndexPO;
import com.yss.valset.extract.repository.mapper.TrIndexRepository;
import com.yss.valset.extract.support.TrIndexStandardizationSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * t_tr_index 标准化落库网关实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TrIndexGatewayImpl implements TrIndexGateway {

    private final TrIndexRepository repository;

    @Override
    public void saveStandardizedIndex(Long taskId, Long fileId, String sourceTp, String sourceSign, ParsedValuationData standardizedValuationData) {
        List<TrIndexPO> rows = TrIndexStandardizationSupport.buildRows(standardizedValuationData, sourceTp, sourceSign);
        if (rows.isEmpty()) {
            log.info("t_tr_index 标准化结果为空，taskId={}, fileId={}, sourceTp={}", taskId, fileId, sourceTp);
            return;
        }
        repository.insertBatchSomeColumn(rows);
        TrIndexPO firstRow = rows.get(0);
        log.info("t_tr_index 标准化落地完成，taskId={}, fileId={}, sourceTp={}, rowCount={}, firstIndxNm={}, firstBizDate={}",
                taskId,
                fileId,
                sourceTp,
                rows.size(),
                firstRow.getIndxNm(),
                firstRow.getBizDate());
    }
}
