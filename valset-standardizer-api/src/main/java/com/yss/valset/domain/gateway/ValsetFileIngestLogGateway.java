package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.ValsetFileIngestLog;

import java.util.List;

/**
 * 文件接入日志网关。
 */
public interface ValsetFileIngestLogGateway {

    /**
     * 保存一条接入日志。
     */
    Long save(ValsetFileIngestLog ingestLog);

    /**
     * 按文件主键查询接入日志。
     */
    List<ValsetFileIngestLog> findByFileId(Long fileId);
}
