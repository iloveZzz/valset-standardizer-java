package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.ValsetMatchResult;

import java.util.List;

/**
 * 主题匹配结果持久化的网关。
 */
public interface MatchResultGateway {
    /**
     * 保留比赛结果。
     */
    void saveResults(Long taskId, Long fileId, List<ValsetMatchResult> results);

    /**
     * 按文件标识查询匹配结果。
     */
    List<ValsetMatchResult> findByFileId(Long fileId);
}
