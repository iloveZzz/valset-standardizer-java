package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.ParsedValuationData;

/**
 * 用于解析工作簿持久性的网关。
 */
public interface ParsedValuationDataGateway {
    /**
     * 保留已解析的工作簿数据。
     */
    void saveParsedValuationData(Long taskId, Long fileId, ParsedValuationData parsedValuationData);

    /**
     * 按文件标识查询最近一次解析后的估值数据。
     */
    ParsedValuationData findLatestByFileId(Long fileId);
}
