package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.ParsedValuationData;

/**
 * STG 外部估值解析快照持久化网关。
 */
public interface StgExternalValuationGateway {

    /**
     * 保存一份解析后的 STG 外部估值解析快照。
     */
    void saveStgExternalValuation(Long taskId, Long fileId, ParsedValuationData parsedValuationData);

    /**
     * 按文件标识查询最近一次落地的 STG 外部估值解析快照。
     */
    ParsedValuationData findLatestByFileId(Long fileId);
}
