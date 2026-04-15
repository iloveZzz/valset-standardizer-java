package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.SubjectMatchFileIngestLog;

import java.util.List;

/**
 * 文件接入日志网关。
 */
public interface SubjectMatchFileIngestLogGateway {

    /**
     * 保存一条接入日志。
     */
    Long save(SubjectMatchFileIngestLog ingestLog);

    /**
     * 按文件主键查询接入日志。
     */
    List<SubjectMatchFileIngestLog> findByFileId(Long fileId);
}
