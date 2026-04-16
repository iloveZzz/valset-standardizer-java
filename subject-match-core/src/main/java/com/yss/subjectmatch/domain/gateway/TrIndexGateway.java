package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.ParsedValuationData;

/**
 * 资产估值指标表持久化网关。
 */
public interface TrIndexGateway {

    /**
     * 将标准化后的指标明细落入 t_tr_index。
     */
    void saveStandardizedIndex(Long taskId, Long fileId, String sourceTp, ParsedValuationData standardizedValuationData);
}
