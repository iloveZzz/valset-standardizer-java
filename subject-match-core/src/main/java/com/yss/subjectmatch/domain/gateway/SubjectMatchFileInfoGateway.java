package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.domain.model.SubjectMatchFileSourceChannel;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件主数据网关。
 */
public interface SubjectMatchFileInfoGateway {

    /**
     * 保存文件主数据并返回主键。
     */
    Long save(SubjectMatchFileInfo fileInfo);

    /**
     * 通过主键加载文件主数据。
     */
    SubjectMatchFileInfo findById(Long fileId);

    /**
     * 按文件指纹查找文件主数据。
     */
    SubjectMatchFileInfo findByFingerprint(String fileFingerprint);

    /**
     * 按条件搜索文件主数据。
     */
    List<SubjectMatchFileInfo> search(SubjectMatchFileSourceChannel sourceChannel,
                                      SubjectMatchFileStatus fileStatus,
                                      String fileFingerprint);

    /**
     * 更新文件状态与最近一次处理信息。
     */
    void updateStatus(Long fileId,
                      SubjectMatchFileStatus fileStatus,
                      Long lastTaskId,
                      LocalDateTime lastProcessedAt,
                      String errorMessage);
}
