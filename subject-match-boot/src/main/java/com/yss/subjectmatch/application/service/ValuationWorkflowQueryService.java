package com.yss.subjectmatch.application.service;

import com.yss.subjectmatch.application.dto.DwdExternalValuationViewDTO;
import com.yss.subjectmatch.application.dto.MatchResultViewDTO;
import com.yss.subjectmatch.application.dto.RawValuationDataViewDTO;

/**
 * 外部估值全流程结果查询服务。
 */
public interface ValuationWorkflowQueryService {
    /**
     * 查询 ODS 原始行数据。
     */
    RawValuationDataViewDTO queryRawData(Long fileId, Integer limit);

    /**
     * 查询 DWD 解析结果。
     */
    DwdExternalValuationViewDTO queryDwdData(Long fileId);

    /**
     * 查询匹配结果。
     */
    MatchResultViewDTO queryMatchResults(Long fileId);
}
