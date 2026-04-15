package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.ParsedValuationData;

/**
 * DWD 外部估值标准数据持久化网关。
 */
public interface DwdExternalValuationGateway {

    /**
     * 保存一份解析后的 DWD 外部估值标准数据快照。
     */
    void saveDwdExternalValuation(Long taskId, Long fileId, ParsedValuationData parsedValuationData);

    /**
     * 按文件标识查询最近一次落地的 DWD 外部估值标准数据。
     */
    ParsedValuationData findLatestByFileId(Long fileId);
}
