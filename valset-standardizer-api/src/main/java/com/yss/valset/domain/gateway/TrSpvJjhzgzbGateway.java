package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.ParsedValuationData;

/**
 * 基金持仓估值标准表持久化网关。
 */
public interface TrSpvJjhzgzbGateway {

    /**
     * 将标准化后的基金持仓估值明细落入 tr_spv_jjhzgzb。
     */
    void saveStandardizedJjhzgzb(Long taskId, Long fileId, String sourceTp, String sourceSign, ParsedValuationData standardizedValuationData);
}
