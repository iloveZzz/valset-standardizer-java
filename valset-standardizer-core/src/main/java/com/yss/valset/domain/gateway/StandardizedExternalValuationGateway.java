package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.ParsedValuationData;

/**
 * 外部估值标准化结果持久化网关。
 */
public interface StandardizedExternalValuationGateway {

    /**
     * 保存标准化后的外部估值事实数据。
     */
    void saveStandardizedExternalValuation(Long valuationId, Long fileId, ParsedValuationData standardizedValuationData);

    /**
     * 按文件标识查询最近一次标准化结果。
     */
    ParsedValuationData findLatestByFileId(Long fileId);
}
