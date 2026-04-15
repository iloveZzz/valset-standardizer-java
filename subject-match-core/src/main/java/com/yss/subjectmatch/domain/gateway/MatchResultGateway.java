package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.SubjectMatchResult;

import java.util.List;

/**
 * 主题匹配结果持久化的网关。
 */
public interface MatchResultGateway {
    /**
     * 保留比赛结果。
     */
    void saveResults(Long taskId, Long fileId, List<SubjectMatchResult> results);

    /**
     * 按文件标识查询匹配结果。
     */
    List<SubjectMatchResult> findByFileId(Long fileId);
}
