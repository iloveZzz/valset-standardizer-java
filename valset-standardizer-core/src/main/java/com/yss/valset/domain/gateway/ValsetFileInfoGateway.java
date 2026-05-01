package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件主数据网关。
 */
public interface ValsetFileInfoGateway {

    /**
     * 保存文件主数据并返回主键。
     */
    Long save(ValsetFileInfo fileInfo);

    /**
     * 通过主键加载文件主数据。
     */
    ValsetFileInfo findById(Long fileId);

    /**
     * 通过路径加载文件主数据。
     */
    ValsetFileInfo findByPath(String path);

    /**
     * 按文件指纹查找文件主数据。
     */
    ValsetFileInfo findByFingerprint(String fileFingerprint);

    /**
     * 按条件搜索文件主数据。
     */
    List<ValsetFileInfo> search(ValsetFileSourceChannel sourceChannel,
                                      ValsetFileStatus fileStatus,
                                      String fileFingerprint);

    /**
     * 更新文件状态与最近一次处理信息。
     */
    void updateStatus(Long fileId,
                      ValsetFileStatus fileStatus,
                      Long lastTaskId,
                      LocalDateTime lastProcessedAt,
                      String errorMessage);

    /**
     * 回写来自 TransferObject 的路径信息。
     */
    void updatePaths(Long fileId,
                     String storageUri,
                     String localTempPath,
                     String realStoragePath);

    /**
     * 使用 TransferObject 的回填结果更新文件主数据。
     */
    void updateFromTransferObject(ValsetFileInfo fileInfo);
}
