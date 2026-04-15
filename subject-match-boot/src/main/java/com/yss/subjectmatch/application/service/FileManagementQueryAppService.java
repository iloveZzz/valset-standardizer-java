package com.yss.subjectmatch.application.service;

import com.yss.subjectmatch.application.dto.SubjectMatchFileIngestLogViewDTO;
import com.yss.subjectmatch.application.dto.SubjectMatchFileInfoViewDTO;
import com.yss.subjectmatch.application.dto.ValuationSheetStyleViewDTO;

import java.util.List;

/**
 * 文件管理查询服务。
 */
public interface FileManagementQueryAppService {

    /**
     * 通过文件主键查询文件主数据。
     */
    SubjectMatchFileInfoViewDTO queryFileInfo(Long fileId);

    /**
     * 按条件搜索文件主数据。
     */
    List<SubjectMatchFileInfoViewDTO> searchFileInfos(String sourceChannel, String fileStatus, String fileFingerprint, Integer limit);

    /**
     * 查询文件接入日志。
     */
    List<SubjectMatchFileIngestLogViewDTO> queryIngestLogs(Long fileId);

    /**
     * 查询 Excel sheet 样式快照。
     */
    List<ValuationSheetStyleViewDTO> querySheetStyles(Long fileId);
}
