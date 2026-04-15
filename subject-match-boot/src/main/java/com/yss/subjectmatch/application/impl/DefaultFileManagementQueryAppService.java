package com.yss.subjectmatch.application.impl;

import com.yss.subjectmatch.application.dto.SubjectMatchFileIngestLogViewDTO;
import com.yss.subjectmatch.application.dto.SubjectMatchFileInfoViewDTO;
import com.yss.subjectmatch.application.service.FileManagementQueryAppService;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileIngestLogGateway;
import com.yss.subjectmatch.domain.model.SubjectMatchFileIngestLog;
import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.domain.model.SubjectMatchFileSourceChannel;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 文件管理查询服务默认实现。
 */
@Slf4j
@Service
public class DefaultFileManagementQueryAppService implements FileManagementQueryAppService {

    private static final int DEFAULT_LIMIT = 50;

    private final SubjectMatchFileInfoGateway subjectMatchFileInfoGateway;
    private final SubjectMatchFileIngestLogGateway subjectMatchFileIngestLogGateway;

    public DefaultFileManagementQueryAppService(SubjectMatchFileInfoGateway subjectMatchFileInfoGateway,
                                               SubjectMatchFileIngestLogGateway subjectMatchFileIngestLogGateway) {
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.subjectMatchFileIngestLogGateway = subjectMatchFileIngestLogGateway;
    }

    @Override
    public SubjectMatchFileInfoViewDTO queryFileInfo(Long fileId) {
        SubjectMatchFileInfo fileInfo = subjectMatchFileInfoGateway.findById(fileId);
        if (fileInfo == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到 fileId 对应的文件主数据: " + fileId);
        }
        return toView(fileInfo);
    }

    @Override
    public List<SubjectMatchFileInfoViewDTO> searchFileInfos(String sourceChannel,
                                                            String fileStatus,
                                                            String fileFingerprint,
                                                            Integer limit) {
        SubjectMatchFileSourceChannel channel = parseSourceChannel(sourceChannel);
        SubjectMatchFileStatus status = parseFileStatus(fileStatus);
        List<SubjectMatchFileInfo> fileInfos = subjectMatchFileInfoGateway.search(channel, status, fileFingerprint);
        if (fileInfos.isEmpty()) {
            return Collections.emptyList();
        }
        int maxSize = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        return fileInfos.stream()
                .limit(maxSize)
                .map(this::toView)
                .toList();
    }

    @Override
    public List<SubjectMatchFileIngestLogViewDTO> queryIngestLogs(Long fileId) {
        List<SubjectMatchFileIngestLog> ingestLogs = subjectMatchFileIngestLogGateway.findByFileId(fileId);
        if (ingestLogs.isEmpty()) {
            return Collections.emptyList();
        }
        return ingestLogs.stream().map(this::toView).toList();
    }

    private SubjectMatchFileInfoViewDTO toView(SubjectMatchFileInfo fileInfo) {
        return SubjectMatchFileInfoViewDTO.builder()
                .fileId(fileInfo.getFileId())
                .fileNameOriginal(fileInfo.getFileNameOriginal())
                .fileNameNormalized(fileInfo.getFileNameNormalized())
                .fileExtension(fileInfo.getFileExtension())
                .mimeType(fileInfo.getMimeType())
                .fileSizeBytes(fileInfo.getFileSizeBytes())
                .fileFingerprint(fileInfo.getFileFingerprint())
                .sourceChannel(fileInfo.getSourceChannel() == null ? null : fileInfo.getSourceChannel().name())
                .sourceUri(fileInfo.getSourceUri())
                .storageType(fileInfo.getStorageType() == null ? null : fileInfo.getStorageType().name())
                .storageUri(fileInfo.getStorageUri())
                .fileFormat(fileInfo.getFileFormat())
                .fileStatus(fileInfo.getFileStatus() == null ? null : fileInfo.getFileStatus().name())
                .createdBy(fileInfo.getCreatedBy())
                .receivedAt(fileInfo.getReceivedAt())
                .storedAt(fileInfo.getStoredAt())
                .lastProcessedAt(fileInfo.getLastProcessedAt())
                .lastTaskId(fileInfo.getLastTaskId())
                .errorMessage(fileInfo.getErrorMessage())
                .sourceMetaJson(fileInfo.getSourceMetaJson())
                .storageMetaJson(fileInfo.getStorageMetaJson())
                .remark(fileInfo.getRemark())
                .build();
    }

    private SubjectMatchFileIngestLogViewDTO toView(SubjectMatchFileIngestLog ingestLog) {
        return SubjectMatchFileIngestLogViewDTO.builder()
                .ingestId(ingestLog.getIngestId())
                .fileId(ingestLog.getFileId())
                .sourceChannel(ingestLog.getSourceChannel() == null ? null : ingestLog.getSourceChannel().name())
                .sourceUri(ingestLog.getSourceUri())
                .channelMessageId(ingestLog.getChannelMessageId())
                .ingestStatus(ingestLog.getIngestStatus())
                .ingestTime(ingestLog.getIngestTime())
                .ingestMetaJson(ingestLog.getIngestMetaJson())
                .createdBy(ingestLog.getCreatedBy())
                .errorMessage(ingestLog.getErrorMessage())
                .build();
    }

    private SubjectMatchFileSourceChannel parseSourceChannel(String sourceChannel) {
        if (sourceChannel == null || sourceChannel.isBlank()) {
            return null;
        }
        try {
            return SubjectMatchFileSourceChannel.valueOf(sourceChannel.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "不支持的 sourceChannel: " + sourceChannel, exception);
        }
    }

    private SubjectMatchFileStatus parseFileStatus(String fileStatus) {
        if (fileStatus == null || fileStatus.isBlank()) {
            return null;
        }
        try {
            return SubjectMatchFileStatus.valueOf(fileStatus.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "不支持的 fileStatus: " + fileStatus, exception);
        }
    }
}
