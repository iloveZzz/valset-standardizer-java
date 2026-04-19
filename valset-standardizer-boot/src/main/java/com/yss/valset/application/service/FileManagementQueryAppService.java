package com.yss.valset.application.service;

import com.yss.valset.application.dto.ValsetFileIngestLogViewDTO;
import com.yss.valset.application.dto.ValsetFileInfoViewDTO;
import com.yss.valset.application.dto.ValuationSheetStyleViewDTO;

import java.util.List;

/**
 * 文件管理查询服务。
 */
public interface FileManagementQueryAppService {

    /**
     * 通过文件主键查询文件主数据。
     */
    ValsetFileInfoViewDTO queryFileInfo(Long fileId);

    /**
     * 按条件搜索文件主数据。
     */
    List<ValsetFileInfoViewDTO> searchFileInfos(String sourceChannel, String fileStatus, String fileFingerprint, Integer limit);

    /**
     * 查询文件接入日志。
     */
    List<ValsetFileIngestLogViewDTO> queryIngestLogs(Long fileId);

    /**
     * 查询 Excel sheet 样式快照。
     */
    List<ValuationSheetStyleViewDTO> querySheetStyles(Long fileId);
}
