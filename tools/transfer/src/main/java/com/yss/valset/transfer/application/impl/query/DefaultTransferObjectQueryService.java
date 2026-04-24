package com.yss.valset.transfer.application.impl.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectPageViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectSourceAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectStatusCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.application.service.TransferObjectQueryService;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectSourceAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectStatusCount;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 默认文件主对象查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferObjectQueryService implements TransferObjectQueryService {

    private final TransferObjectGateway transferObjectGateway;
    private final ObjectMapper objectMapper;

    @Override
    public TransferObjectViewDTO getObject(String transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文件主对象，transferId=" + transferId));
        return toView(transferObject);
    }

    @Override
    public PageResult<TransferObjectViewDTO> pageObjects(String sourceId, String sourceType, String sourceCode, String status, String mailId, String fingerprint, String routeId, Integer pageIndex, Integer pageSize) {
        String normalizedStatus = normalizeStatus(status);
        TransferObjectPage page = transferObjectGateway.pageObjects(sourceId, sourceType, sourceCode, normalizedStatus, mailId, fingerprint, routeId, pageIndex, pageSize);
        List<TransferObjectViewDTO> data = page.records() == null ? List.of() : page.records().stream().map(this::toView).collect(Collectors.toList());
        return PageResult.of(data,
                        page.total(),
                        page.pageSize(),
                pageIndex
                );
    }

    @Override
    public TransferObjectAnalysisViewDTO analyzeObjects(String sourceId,
                                                        String sourceType,
                                                        String sourceCode,
                                                        String status,
                                                        String mailId,
                                                        String fingerprint,
                                                        String routeId) {
        String normalizedStatus = normalizeStatus(status);
        TransferObjectAnalysis analysis = transferObjectGateway.analyzeObjects(sourceId, sourceType, sourceCode, normalizedStatus, mailId, fingerprint, routeId);
        return TransferObjectAnalysisViewDTO.builder()
                .totalCount(analysis.totalCount())
                .sourceAnalyses(analysis.sourceAnalyses() == null ? List.of() : analysis.sourceAnalyses().stream().map(this::toSourceAnalysisView).toList())
                .build();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return TransferStatus.valueOf(status.trim().toUpperCase()).name();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的文件状态: " + status, exception);
        }
    }

    private TransferObjectSourceAnalysisViewDTO toSourceAnalysisView(TransferObjectSourceAnalysis sourceAnalysis) {
        return TransferObjectSourceAnalysisViewDTO.builder()
                .sourceType(sourceAnalysis.sourceType())
                .totalCount(sourceAnalysis.totalCount())
                .statusCounts(sourceAnalysis.statusCounts() == null ? List.of() : sourceAnalysis.statusCounts().stream().map(this::toStatusCountView).toList())
                .build();
    }

    private TransferObjectStatusCountViewDTO toStatusCountView(TransferObjectStatusCount statusCount) {
        return TransferObjectStatusCountViewDTO.builder()
                .status(statusCount.status())
                .statusLabel(resolveStatusLabel(statusCount.status()))
                .count(statusCount.count())
                .build();
    }

    private String resolveStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "-";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "待处理";
            case "RECEIVED" -> "已收取";
            case "IDENTIFIED" -> "已识别";
            case "ROUTED" -> "已路由";
            case "DELIVERING" -> "投递中";
            case "DELIVERED" -> "已投递";
            case "ARCHIVED" -> "已归档";
            case "SKIPPED" -> "已跳过";
            case "QUARANTINED" -> "已隔离";
            case "FAILED" -> "失败";
            default -> status;
        };
    }

    private TransferObjectViewDTO toView(TransferObject transferObject) {
        return TransferObjectViewDTO.builder()
                .transferId(transferObject.transferId() == null ? null : String.valueOf(transferObject.transferId()))
                .sourceId(transferObject.sourceId() == null ? null : String.valueOf(transferObject.sourceId()))
                .sourceType(transferObject.sourceType())
                .sourceCode(transferObject.sourceCode())
                .originalName(transferObject.originalName())
                .extension(transferObject.extension())
                .mimeType(transferObject.mimeType())
                .sizeBytes(transferObject.sizeBytes() == null ? null : String.valueOf(transferObject.sizeBytes()))
                .fingerprint(transferObject.fingerprint())
                .sourceRef(transferObject.sourceRef())
                .mailId(transferObject.mailId())
                .mailFrom(transferObject.mailFrom())
                .mailTo(transferObject.mailTo())
                .mailCc(transferObject.mailCc())
                .mailBcc(transferObject.mailBcc())
                .mailSubject(transferObject.mailSubject())
                .mailBody(transferObject.mailBody())
                .mailProtocol(transferObject.mailProtocol())
                .mailFolder(transferObject.mailFolder())
                .localTempPath(transferObject.localTempPath())
                .status(transferObject.status() == null ? null : transferObject.status().name())
                .receivedAt(transferObject.receivedAt() == null ? null : java.time.LocalDateTime.ofInstant(transferObject.receivedAt(), java.time.ZoneId.systemDefault()))
                .storedAt(transferObject.storedAt() == null ? null : java.time.LocalDateTime.ofInstant(transferObject.storedAt(), java.time.ZoneId.systemDefault()))
                .routeId(transferObject.routeId() == null ? null : String.valueOf(transferObject.routeId()))
                .errorMessage(transferObject.errorMessage())
                .fileMetaJson(toJson(transferObject.fileMeta()))
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("文件元数据序列化失败", exception);
        }
    }
}
