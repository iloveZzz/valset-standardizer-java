package com.yss.valset.file.application.service;

import com.yss.valset.file.application.dto.ValsetFileIngestLogViewDTO;
import com.yss.valset.file.application.dto.ValsetFileInfoViewDTO;
import com.yss.valset.file.application.dto.ValuationSheetStyleViewDTO;

import java.util.List;

/**
 * 文件管理查询服务。
 */
public interface FileManagementQueryAppService {

    ValsetFileInfoViewDTO queryFileInfo(Long fileId);

    ValsetFileInfoViewDTO queryFileInfoByPath(String path);

    List<ValsetFileInfoViewDTO> searchFileInfos(String sourceChannel, String fileStatus, String fileFingerprint, Integer limit);

    List<ValsetFileIngestLogViewDTO> queryIngestLogs(Long fileId);

    List<ValsetFileIngestLogViewDTO> queryIngestLogsByPath(String path);

    List<ValuationSheetStyleViewDTO> querySheetStyles(Long fileId);

    List<ValuationSheetStyleViewDTO> querySheetStylesByPath(String path);
}
