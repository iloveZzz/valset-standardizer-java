package com.yss.valset.application.service;

import com.yss.valset.application.dto.MatchResultViewDTO;
import com.yss.valset.application.dto.RawValuationDataViewDTO;
import com.yss.valset.application.dto.StgExternalValuationViewDTO;

/**
 * 外部估值全流程结果查询服务。
 */
public interface ValuationWorkflowQueryService {
    /**
     * 查询 ODS 原始行数据。
     */
    RawValuationDataViewDTO queryRawData(Long fileId, Integer limit);

    /**
     * 查询 STG 解析快照。
     */
    StgExternalValuationViewDTO queryStgData(Long fileId);

    /**
     * 查询匹配结果。
     */
    MatchResultViewDTO queryMatchResults(Long fileId);
}
