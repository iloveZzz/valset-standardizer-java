package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.ParsedValuationData;

/**
 * 基金持仓估值标准表持久化网关。
 */
public interface DwdJjhzgzbGateway {

    /**
     * 将标准化后的基金持仓估值明细落入 t_tr_jjhzgzb。
     */
    void saveStandardizedJjhzgzb(Long taskId, Long fileId, String sourceTp, ParsedValuationData standardizedValuationData);
}
