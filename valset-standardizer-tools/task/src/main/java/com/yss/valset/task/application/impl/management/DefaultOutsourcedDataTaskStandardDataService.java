package com.yss.valset.task.application.impl.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.extract.support.ExcelUniverSnapshotSupport;
import com.yss.valset.extract.repository.entity.DwdExternalValuationBasicInfoPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationHeaderPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationMetricPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationSubjectPO;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationBasicInfoRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationHeaderRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationMetricRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationSubjectRepository;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.task.application.command.OutsourcedDataTaskStandardDataExportCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskRawWorkbookDownloadDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskRawWorkbookDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardBasicDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardBasicRowDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardDataExportDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardMetricDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardRawColumnDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardSubjectDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.application.service.OutsourcedDataTaskStandardDataService;
import com.yss.valset.task.application.support.UniverWorkbookExportSupport;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.infrastructure.connector.SourceConnectorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 默认估值解析批次标准数据查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultOutsourcedDataTaskStandardDataService implements OutsourcedDataTaskStandardDataService {

    private static final TypeReference<Object> RAW_VALUES_TYPE = new TypeReference<Object>() {
    };
    private static final Set<String> RAW_COLUMN_EXCLUDED_HEADERS;

    static {
        Set<String> headers = new LinkedHashSet<>();
        headers.add("科目编码");
        headers.add("科目代码");
        headers.add("科编码");
        headers.add("科目名称");
        RAW_COLUMN_EXCLUDED_HEADERS = Collections.unmodifiableSet(headers);
    }

    private final OutsourcedDataTaskGateway outsourcedDataTaskGateway;
    private final DwdExternalValuationRepository valuationRepository;
    private final DwdExternalValuationBasicInfoRepository basicInfoRepository;
    private final DwdExternalValuationHeaderRepository headerRepository;
    private final DwdExternalValuationSubjectRepository subjectRepository;
    private final DwdExternalValuationMetricRepository metricRepository;
    private final DatabaseDialectSupport databaseDialectSupport;
    private final ObjectMapper objectMapper;
    private final UniverWorkbookExportSupport univerWorkbookExportSupport;
    private final ValsetFileInfoGateway valsetFileInfoGateway;
    private final TransferObjectGateway transferObjectGateway;
    private final TransferSourceGateway transferSourceGateway;
    private final SourceConnectorRegistry sourceConnectorRegistry;

    @Override
    public OutsourcedDataTaskStandardBasicDTO queryBasic(String batchId) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        DwdExternalValuationPO valuation = findValuation(batch);
        if (valuation == null) {
            return emptyBasic(batch);
        }
        Long valuationId = valuation.getId();
        List<DwdExternalValuationBasicInfoPO> basicInfos = basicInfoRepository.selectList(
                Wrappers.lambdaQuery(DwdExternalValuationBasicInfoPO.class)
                        .eq(DwdExternalValuationBasicInfoPO::getValuationId, valuationId)
                        .orderByAsc(DwdExternalValuationBasicInfoPO::getSortOrder)
                        .orderByAsc(DwdExternalValuationBasicInfoPO::getId)
        );
        OutsourcedDataTaskStandardBasicDTO dto = new OutsourcedDataTaskStandardBasicDTO();
        dto.setBatchId(batchId);
        dto.setValuationId(valuationId);
        dto.setFileId(valuation.getFileId());
        dto.setTaskId(valuation.getTaskId());
        dto.setWorkbookPath(valuation.getWorkbookPath());
        dto.setSheetName(valuation.getSheetName());
        dto.setTitle(valuation.getTitle());
        dto.setHeaderRowNumber(valuation.getHeaderRowNumber());
        dto.setDataStartRowNumber(valuation.getDataStartRowNumber());
        dto.setBasicInfoCount((long) basicInfos.size());
        dto.setSubjectCount(subjectRepository.selectCount(
                Wrappers.lambdaQuery(DwdExternalValuationSubjectPO.class)
                        .eq(DwdExternalValuationSubjectPO::getValuationId, valuationId)
        ));
        dto.setMetricCount(metricRepository.selectCount(
                Wrappers.lambdaQuery(DwdExternalValuationMetricPO.class)
                        .eq(DwdExternalValuationMetricPO::getValuationId, valuationId)
        ));
        dto.setBasicRows(buildBasicRows(valuation, basicInfos));
        dto.setRawColumns(loadRawColumns(valuationId));
        return dto;
    }

    @Override
    public List<OutsourcedDataTaskStandardSubjectDTO> listSubjects(String batchId, String keyword) {
        DwdExternalValuationPO valuation = findValuation(requireBatch(batchId));
        if (valuation == null) {
            return Collections.emptyList();
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        LambdaQueryWrapper<DwdExternalValuationSubjectPO> query = Wrappers.lambdaQuery(DwdExternalValuationSubjectPO.class)
                .eq(DwdExternalValuationSubjectPO::getValuationId, valuation.getId())
                .and(StringUtils.hasText(normalizedKeyword), wrapper -> wrapper
                        .like(DwdExternalValuationSubjectPO::getSubjectCode, normalizedKeyword)
                        .or()
                        .like(DwdExternalValuationSubjectPO::getSubjectName, normalizedKeyword))
                .orderByAsc(DwdExternalValuationSubjectPO::getRowDataNumber)
                .orderByAsc(DwdExternalValuationSubjectPO::getId);
        List<DwdExternalValuationSubjectPO> records = subjectRepository.selectList(query);
        List<OutsourcedDataTaskStandardRawColumnDTO> rawColumns = loadRawColumns(valuation.getId());
        return records == null
                ? Collections.emptyList()
                : records.stream()
                        .map(po -> toSubjectDTO(po, rawColumns))
                        .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<OutsourcedDataTaskStandardMetricDTO> listMetrics(String batchId, String keyword) {
        DwdExternalValuationPO valuation = findValuation(requireBatch(batchId));
        if (valuation == null) {
            return Collections.emptyList();
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        LambdaQueryWrapper<DwdExternalValuationMetricPO> query = Wrappers.lambdaQuery(DwdExternalValuationMetricPO.class)
                .eq(DwdExternalValuationMetricPO::getValuationId, valuation.getId())
                .and(StringUtils.hasText(normalizedKeyword), wrapper -> wrapper
                        .like(DwdExternalValuationMetricPO::getMetricName, normalizedKeyword)
                        .or()
                        .like(DwdExternalValuationMetricPO::getMetricType, normalizedKeyword)
                        .or()
                        .like(DwdExternalValuationMetricPO::getMetricValue, normalizedKeyword))
                .orderByAsc(DwdExternalValuationMetricPO::getRowDataNumber)
                .orderByAsc(DwdExternalValuationMetricPO::getId);
        List<DwdExternalValuationMetricPO> records = metricRepository.selectList(query);
        List<OutsourcedDataTaskStandardRawColumnDTO> rawColumns = loadRawColumns(valuation.getId());
        return records == null
                ? Collections.emptyList()
                : records.stream()
                        .map(po -> toMetricDTO(po, rawColumns))
                        .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public OutsourcedDataTaskRawWorkbookDTO queryRawWorkbook(String batchId) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        RawWorkbookSource source = resolveRawWorkbookSource(batch);
        try (ExcelUniverSnapshotSupport snapshotSupport = new ExcelUniverSnapshotSupport(source.path)) {
            ExcelUniverSnapshotSupport.WorkbookSnapshot snapshot = snapshotSupport.buildWorkbookSnapshot(source.fileName);
            OutsourcedDataTaskRawWorkbookDTO dto = new OutsourcedDataTaskRawWorkbookDTO();
            dto.setBatchId(batch.getBatchId());
            dto.setFileId(source.fileId);
            dto.setFileName(source.fileName);
            dto.setSourceType(source.sourceType);
            dto.setSheetCount(snapshot.getSheetCount());
            dto.setRowCount(snapshot.getRowCount());
            dto.setWorkbookData(objectMapper.valueToTree(snapshot.getWorkbookData()));
            dto.setDownloadedFromTarget(source.downloadedFromTarget);
            dto.setFallbackMessage(source.fallbackMessage);
            return dto;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原始估值表转换失败", exception);
        }
    }

    @Override
    public OutsourcedDataTaskRawWorkbookDownloadDTO downloadRawWorkbook(String batchId) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        RawWorkbookSource source = resolveRawWorkbookSource(batch);
        try {
            return new OutsourcedDataTaskRawWorkbookDownloadDTO(
                    batch.getBatchId(),
                    source.fileId,
                    source.fileName,
                    resolveContentType(source.mimeType, source.path),
                    Files.size(source.path),
                    source.path
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原始估值表下载准备失败", exception);
        }
    }

    @Override
    public OutsourcedDataTaskStandardDataExportDTO exportSheet(String batchId, OutsourcedDataTaskStandardDataExportCommand command) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        if (command == null || isEmptyWorkbook(command.getWorkbookData())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出工作簿快照不能为空");
        }
        try {
            byte[] content = univerWorkbookExportSupport.export(command.getWorkbookData(), command.getSheetName());
            return new OutsourcedDataTaskStandardDataExportDTO(
                    buildExportFileName(batch, command.getTab(), command.getSheetName()),
                    content
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导出标准数据 Sheet 失败", exception);
        }
    }

    private RawWorkbookSource resolveRawWorkbookSource(OutsourcedDataTaskBatchDTO batch) {
        Long fileId = parseLong(batch.getFileId());
        DwdExternalValuationPO valuation = findValuation(batch);
        ValsetFileInfo fileInfo = fileId == null ? null : valsetFileInfoGateway.findById(fileId);
        TransferObject transferObject = fileId == null ? null : transferObjectGateway.findById(String.valueOf(fileId)).orElse(null);
        String fileName = firstText(
                batch.getOriginalFileName(),
                fileInfo == null ? null : fileInfo.getFileNameOriginal(),
                transferObject == null ? null : transferObject.originalName(),
                "原始估值表"
        );
        String mimeType = firstText(
                transferObject == null ? null : transferObject.mimeType(),
                fileInfo == null ? null : fileInfo.getMimeType()
        );
        String sourceType = firstText(
                batch.getSourceType(),
                transferObject == null ? null : transferObject.sourceType(),
                fileInfo == null || fileInfo.getSourceChannel() == null ? null : fileInfo.getSourceChannel().name()
        );
        Path localPath = firstReadablePath(
                fileInfo == null ? null : fileInfo.getLocalTempPath(),
                valuation == null ? null : valuation.getWorkbookPath(),
                fileInfo == null ? null : fileInfo.getRealStoragePath(),
                fileInfo == null ? null : fileInfo.getStorageUri(),
                transferObject == null ? null : transferObject.localTempPath(),
                transferObject == null ? null : transferObject.realStoragePath()
        );
        if (localPath != null) {
            return new RawWorkbookSource(localPath, fileId, fileName, mimeType, sourceType, false, null);
        }
        if (transferObject == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "批次没有可定位的源文件");
        }
        Path materialized = materializeFromSource(transferObject);
        String fallbackMessage = "本地临时文件不可读，已从来源配置重新下载";
        return new RawWorkbookSource(materialized, fileId, fileName, mimeType, sourceType, true, fallbackMessage);
    }

    private Path materializeFromSource(TransferObject transferObject) {
        if (!StringUtils.hasText(transferObject.sourceId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批次源文件不可读，且缺少来源配置");
        }
        TransferSource source = transferSourceGateway.findById(transferObject.sourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到批次关联的来源配置"));
        try {
            Path path = sourceConnectorRegistry.getRequired(source).materialize(source, transferObject);
            if (!isReadableFile(path)) {
                throw new IllegalStateException("来源连接器未返回可读文件，path=" + path);
            }
            return path;
        } catch (UnsupportedOperationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前来源不支持重新下载原始文件", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标源下载原始估值表失败", exception);
        }
    }

    private Path firstReadablePath(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            try {
                Path path = Paths.get(candidate.trim());
                if (isReadableFile(path)) {
                    return path;
                }
            } catch (Exception ignored) {
                // ignore invalid local path candidates
            }
        }
        return null;
    }

    private boolean isReadableFile(Path path) {
        return path != null && Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }

    private String resolveContentType(String mimeType, Path filePath) {
        if (StringUtils.hasText(mimeType)) {
            return mimeType.trim();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
        } catch (Exception exception) {
            return "application/octet-stream";
        }
    }

    private static final class RawWorkbookSource {
        private final Path path;
        private final Long fileId;
        private final String fileName;
        private final String mimeType;
        private final String sourceType;
        private final boolean downloadedFromTarget;
        private final String fallbackMessage;

        private RawWorkbookSource(Path path, Long fileId, String fileName, String mimeType, String sourceType,
                boolean downloadedFromTarget, String fallbackMessage) {
            this.path = path;
            this.fileId = fileId;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.sourceType = sourceType;
            this.downloadedFromTarget = downloadedFromTarget;
            this.fallbackMessage = fallbackMessage;
        }
    }

    private OutsourcedDataTaskBatchDTO requireBatch(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批次ID不能为空");
        }
        return outsourcedDataTaskGateway.findTask(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到批次对应的估值解析任务"));
    }

    private boolean isEmptyWorkbook(JsonNode workbookData) {
        return workbookData == null || workbookData.isNull() || workbookData.isMissingNode() || !workbookData.isObject();
    }

    private String buildExportFileName(OutsourcedDataTaskBatchDTO batch, String tab, String sheetName) {
        String batchName = firstText(batch.getBatchName(), batch.getBatchId(), "标准数据");
        String tabName = standardDataTabName(tab, sheetName);
        return sanitizeFileName("估值标准数据_" + batchName + "_" + tabName + ".xlsx");
    }

    private String standardDataTabName(String tab, String sheetName) {
        String normalized = String.valueOf(tab == null ? "" : tab).trim().toLowerCase();
        if ("basic".equals(normalized)) {
            return "基础信息";
        }
        if ("subjects".equals(normalized)) {
            return "估值明细";
        }
        if ("metrics".equals(normalized)) {
            return "指标数据";
        }
        if ("raw".equals(normalized)) {
            return "原始估值表";
        }
        return StringUtils.hasText(sheetName) ? sheetName.trim() : "Sheet";
    }

    private String firstText(String first, String second, String defaultValue) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return defaultValue;
    }

    private String firstText(String first, String second) {
        return firstText(first, second, null);
    }

    private String firstText(String first, String second, String third, String defaultValue) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        if (StringUtils.hasText(third)) {
            return third.trim();
        }
        return defaultValue;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private DwdExternalValuationPO findValuation(OutsourcedDataTaskBatchDTO batch) {
        return findLatestValuation(parseLong(batch.getFileId()), batch.getTaskId());
    }

    private OutsourcedDataTaskStandardBasicDTO emptyBasic(OutsourcedDataTaskBatchDTO batch) {
        OutsourcedDataTaskStandardBasicDTO dto = new OutsourcedDataTaskStandardBasicDTO();
        dto.setBatchId(batch.getBatchId());
        dto.setFileId(parseLong(batch.getFileId()));
        dto.setTaskId(batch.getTaskId());
        dto.setBasicInfoCount(0L);
        dto.setSubjectCount(0L);
        dto.setMetricCount(0L);
        dto.setBasicRows(Collections.emptyList());
        dto.setRawColumns(Collections.emptyList());
        return dto;
    }

    private DwdExternalValuationPO findLatestValuation(Long fileId, Long taskId) {
        DwdExternalValuationPO valuation = null;
        if (fileId != null) {
            valuation = valuationRepository.selectOne(
                    Wrappers.lambdaQuery(DwdExternalValuationPO.class)
                            .eq(DwdExternalValuationPO::getFileId, fileId)
                            .orderByDesc(DwdExternalValuationPO::getId)
                            .last(databaseDialectSupport.limitClause(1))
            );
        }
        if (valuation == null && taskId != null) {
            valuation = valuationRepository.selectOne(
                    Wrappers.lambdaQuery(DwdExternalValuationPO.class)
                            .eq(DwdExternalValuationPO::getTaskId, taskId)
                            .orderByDesc(DwdExternalValuationPO::getId)
                            .last(databaseDialectSupport.limitClause(1))
            );
        }
        return valuation;
    }

    private List<OutsourcedDataTaskStandardBasicRowDTO> buildBasicRows(DwdExternalValuationPO valuation,
            List<DwdExternalValuationBasicInfoPO> basicInfos) {
        List<OutsourcedDataTaskStandardBasicRowDTO> rows = new ArrayList<>();
        appendBasicRow(rows, "主表信息", "估值ID", stringValue(valuation.getId()));
        appendBasicRow(rows, "主表信息", "文件ID", stringValue(valuation.getFileId()));
        appendBasicRow(rows, "主表信息", "任务ID", stringValue(valuation.getTaskId()));
        appendBasicRow(rows, "主表信息", "工作簿路径", valuation.getWorkbookPath());
        appendBasicRow(rows, "主表信息", "Sheet名称", valuation.getSheetName());
        appendBasicRow(rows, "主表信息", "标题", valuation.getTitle());
        appendBasicRow(rows, "主表信息", "表头行号", stringValue(valuation.getHeaderRowNumber()));
        appendBasicRow(rows, "主表信息", "数据起始行号", stringValue(valuation.getDataStartRowNumber()));
        for (DwdExternalValuationBasicInfoPO basicInfo : basicInfos) {
            appendBasicRow(rows, "基础信息", basicInfo.getInfoKey(), basicInfo.getInfoValue());
        }
        return rows;
    }

    private void appendBasicRow(List<OutsourcedDataTaskStandardBasicRowDTO> rows, String category,
            String fieldName, String fieldValue) {
        OutsourcedDataTaskStandardBasicRowDTO row = new OutsourcedDataTaskStandardBasicRowDTO();
        row.setCategory(category);
        row.setFieldName(fieldName);
        row.setFieldValue(fieldValue);
        rows.add(row);
    }

    private List<OutsourcedDataTaskStandardRawColumnDTO> loadRawColumns(Long valuationId) {
        List<DwdExternalValuationHeaderPO> headers = headerRepository.selectList(
                Wrappers.lambdaQuery(DwdExternalValuationHeaderPO.class)
                        .eq(DwdExternalValuationHeaderPO::getValuationId, valuationId)
                        .orderByAsc(DwdExternalValuationHeaderPO::getColumnIndex)
                        .orderByAsc(DwdExternalValuationHeaderPO::getId)
        );
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyList();
        }
        List<OutsourcedDataTaskStandardRawColumnDTO> columns = new ArrayList<>();
        for (DwdExternalValuationHeaderPO header : headers) {
            Integer columnIndex = header.getColumnIndex();
            if (columnIndex == null || columnIndex < 2) {
                continue;
            }
            String title = normalizeHeaderTitle(header.getHeaderName());
            if (!StringUtils.hasText(title) || isExcludedRawColumn(title)) {
                continue;
            }
            OutsourcedDataTaskStandardRawColumnDTO column = new OutsourcedDataTaskStandardRawColumnDTO();
            column.setColumnIndex(columnIndex);
            column.setTitle(title);
            column.setFieldKey(rawColumnFieldKey(columnIndex, title));
            columns.add(column);
        }
        return columns;
    }

    private OutsourcedDataTaskStandardSubjectDTO toSubjectDTO(DwdExternalValuationSubjectPO po,
            List<OutsourcedDataTaskStandardRawColumnDTO> rawColumns) {
        OutsourcedDataTaskStandardSubjectDTO dto = new OutsourcedDataTaskStandardSubjectDTO();
        dto.setId(po.getId());
        dto.setValuationId(po.getValuationId());
        dto.setSheetName(po.getSheetName());
        dto.setRowDataNumber(po.getRowDataNumber());
        dto.setSubjectCode(po.getSubjectCode());
        dto.setSubjectName(po.getSubjectName());
        dto.setLevelNo(po.getLevelNo());
        dto.setParentCode(po.getParentCode());
        dto.setRootCode(po.getRootCode());
        dto.setLeaf(po.getLeaf());
        dto.setRawValuesJson(po.getRawValuesJson());
        dto.setRawValues(buildRawValues(po.getRawValuesJson(), rawColumns));
        return dto;
    }

    private OutsourcedDataTaskStandardMetricDTO toMetricDTO(DwdExternalValuationMetricPO po,
            List<OutsourcedDataTaskStandardRawColumnDTO> rawColumns) {
        OutsourcedDataTaskStandardMetricDTO dto = new OutsourcedDataTaskStandardMetricDTO();
        dto.setId(po.getId());
        dto.setValuationId(po.getValuationId());
        dto.setSheetName(po.getSheetName());
        dto.setRowDataNumber(po.getRowDataNumber());
        dto.setMetricName(po.getMetricName());
        dto.setMetricType(po.getMetricType());
        dto.setMetricValue(po.getMetricValue());
        dto.setRawValuesJson(po.getRawValuesJson());
        dto.setRawValues(buildRawValues(po.getRawValuesJson(), rawColumns));
        return dto;
    }

    private Map<String, String> buildRawValues(String rawValuesJson,
            List<OutsourcedDataTaskStandardRawColumnDTO> rawColumns) {
        Object source = readRawValues(rawValuesJson);
        if (source == null) {
            return Collections.emptyMap();
        }
        if (rawColumns == null || rawColumns.isEmpty()) {
            return buildFallbackRawValues(source);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (OutsourcedDataTaskStandardRawColumnDTO column : rawColumns) {
            values.put(column.getFieldKey(), stringValue(resolveRawValue(source, column)));
        }
        return values;
    }

    private Object readRawValues(String rawValuesJson) {
        if (!StringUtils.hasText(rawValuesJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawValuesJson, RAW_VALUES_TYPE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> buildFallbackRawValues(Object source) {
        Map<String, String> fallback = new LinkedHashMap<>();
        if (!(source instanceof Map)) {
            return fallback;
        }
        Map<?, ?> sourceMap = (Map<?, ?>) source;
        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            String title = normalizeHeaderTitle(stringValue(entry.getKey()));
            if (!StringUtils.hasText(title) || isExcludedRawColumn(title)) {
                continue;
            }
            fallback.put(title, stringValue(entry.getValue()));
        }
        return fallback;
    }

    private Object resolveRawValue(Object source, OutsourcedDataTaskStandardRawColumnDTO column) {
        if (source instanceof List) {
            Integer columnIndex = column.getColumnIndex();
            List<?> sourceList = (List<?>) source;
            if (columnIndex != null && columnIndex >= 0 && columnIndex < sourceList.size()) {
                return sourceList.get(columnIndex);
            }
            return null;
        }
        if (source instanceof Map) {
            Map<?, ?> sourceMap = (Map<?, ?>) source;
            Object value = sourceMap.get(column.getTitle());
            if (value != null) {
                return value;
            }
            Integer columnIndex = column.getColumnIndex();
            if (columnIndex != null) {
                value = sourceMap.get(String.valueOf(columnIndex));
                if (value != null) {
                    return value;
                }
                return sourceMap.get(rawColumnFieldKey(columnIndex, column.getTitle()));
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeHeaderTitle(String headerName) {
        return headerName == null ? null : headerName.trim();
    }

    private boolean isExcludedRawColumn(String headerName) {
        String normalized = headerName == null ? "" : headerName.replaceAll("\\s+", "");
        return RAW_COLUMN_EXCLUDED_HEADERS.contains(normalized);
    }

    private String rawColumnFieldKey(Integer columnIndex, String title) {
        return columnIndex == null ? title : "raw_" + columnIndex;
    }
}
