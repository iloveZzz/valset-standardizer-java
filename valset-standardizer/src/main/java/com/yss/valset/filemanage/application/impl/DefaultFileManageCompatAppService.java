package com.yss.valset.filemanage.application.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.filemanage.application.dto.FileReceiveVO;
import com.yss.valset.filemanage.application.dto.FileStateGroupVO;
import com.yss.valset.filemanage.application.dto.SourceFileManagePage;
import com.yss.valset.filemanage.application.dto.SourceFileManageQuery;
import com.yss.valset.filemanage.application.dto.SourceFileManageVO;
import com.yss.valset.filemanage.application.dto.SourceFileResetCmd;
import com.yss.valset.filemanage.application.service.FileManageCompatAppService;
import com.yss.valset.parser.application.command.ParseQueueGenerateCommand;
import com.yss.valset.parser.application.command.ParseQueueRetryCommand;
import com.yss.valset.parser.application.service.ParseQueueManagementAppService;
import com.yss.valset.parser.infrastructure.entity.ParseQueuePO;
import com.yss.valset.parser.infrastructure.mapper.ParseQueueRepository;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.service.OutsourcedDataTaskService;
import com.yss.valset.transfer.application.command.TransferObjectRetagCommand;
import com.yss.valset.transfer.application.dto.TransferObjectDownloadViewDTO;
import com.yss.valset.transfer.application.dto.TransferSourceMutationResponse;
import com.yss.valset.transfer.application.service.TransferObjectManagementAppService;
import com.yss.valset.transfer.application.service.TransferObjectQueryService;
import com.yss.valset.transfer.application.service.TransferSourceManagementAppService;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectTagPO;
import com.yss.valset.transfer.infrastructure.entity.TransferSourcePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectTagRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * 默认文件收取管理前端兼容应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFileManageCompatAppService implements FileManageCompatAppService {

    private static final String STATE_UNMATCHED = "UNMATCHED";
    private static final String STATE_PENDING = "PENDING";
    private static final String STATE_ANALYZED = "ANALYZED";
    private static final String STATE_ANALYZE_EXCEPTION = "ANALYZE_EXCEPTION";
    private static final String STATE_INVALID = "INVALID";
    private static final List<String> STATE_ORDER = Arrays.asList(
            STATE_UNMATCHED,
            STATE_PENDING,
            STATE_ANALYZED,
            STATE_ANALYZE_EXCEPTION,
            STATE_INVALID
    );
    private static final Map<String, String> STATE_NAMES;
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DOWNLOAD_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int BATCH_DOWNLOAD_LIMIT = 500;

    static {
        Map<String, String> names = new LinkedHashMap<>();
        names.put(STATE_UNMATCHED, "未匹配");
        names.put(STATE_PENDING, "待分析");
        names.put(STATE_ANALYZED, "已分析");
        names.put(STATE_ANALYZE_EXCEPTION, "分析异常");
        names.put(STATE_INVALID, "已失效");
        STATE_NAMES = Collections.unmodifiableMap(names);
    }

    private final TransferObjectRepository transferObjectRepository;
    private final TransferObjectTagRepository transferObjectTagRepository;
    private final TransferSourceRepository transferSourceRepository;
    private final ParseQueueRepository parseQueueRepository;
    private final TransferSourceManagementAppService transferSourceManagementAppService;
    private final TransferObjectQueryService transferObjectQueryService;
    private final TransferObjectManagementAppService transferObjectManagementAppService;
    private final ParseQueueManagementAppService parseQueueManagementAppService;
    private final OutsourcedDataTaskService outsourcedDataTaskService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<SourceFileManageVO> page(SourceFileManagePage query) {
        List<SourceFileManageVO> allRows = buildRows(query);
        int pageIndex = safePageIndex(query == null ? null : query.getPageIndex());
        int pageSize = safePageSize(query == null ? null : query.getPageSize());
        int fromIndex = Math.min((pageIndex - 1) * pageSize, allRows.size());
        int toIndex = Math.min(fromIndex + pageSize, allRows.size());
        return PageResult.of(allRows.subList(fromIndex, toIndex), allRows.size(), pageSize, pageIndex);
    }

    @Override
    public List<FileStateGroupVO> stateGroups(SourceFileManageQuery query) {
        Map<String, Long> counts = buildRows(query, false).stream()
                .collect(Collectors.groupingBy(
                        row -> resolveStateCode(row.getFileState()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        List<FileStateGroupVO> result = new ArrayList<>();
        for (String code : STATE_ORDER) {
            Long count = counts.get(code);
            if (count != null && count > 0) {
                result.add(FileStateGroupVO.builder()
                        .code(code)
                        .groupName(STATE_NAMES.get(code))
                        .groupCount(count)
                        .build());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileReceiveVO uploadBatch(List<MultipartFile> files) {
        List<MultipartFile> validFiles = normalizeFiles(files);
        if (validFiles.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "请选择要上传的文件");
        }
        TransferSourcePO source = resolveManualUploadSource();
        TransferSourceMutationResponse response = transferSourceManagementAppService.uploadFiles(source.getSourceId(), validFiles);
        log.info("文件收取管理手动上传已委托 HTTP 来源，sourceId={}, message={}", source.getSourceId(), response == null ? null : response.getMessage());
        return FileReceiveVO.builder()
                .totalNum(validFiles.size())
                .matchedNum(0)
                .unMatcheNum(0)
                .rejectNum(0)
                .lockedNum(0)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileReceiveVO ingestByDate(String startDate) {
        LocalDate receiveDate = parseBasicDate(startDate);
        LocalDateTime before = LocalDateTime.now();
        int triggered = 0;
        int locked = 0;
        int rejected = 0;
        for (TransferSourcePO source : listEnabledSources()) {
            try {
                transferSourceManagementAppService.triggerSource(source.getSourceId());
                triggered++;
            } catch (ResponseStatusException exception) {
                if (exception.getStatus() == CONFLICT) {
                    locked++;
                } else {
                    rejected++;
                    log.warn("手动收取来源触发失败，sourceId={}, startDate={}", source.getSourceId(), startDate, exception);
                }
            } catch (RuntimeException exception) {
                rejected++;
                log.warn("手动收取来源触发失败，sourceId={}, startDate={}", source.getSourceId(), startDate, exception);
            }
        }
        int total = countReceivedObjects(receiveDate, before);
        int matched = countMatchedObjects(receiveDate, before);
        return FileReceiveVO.builder()
                .totalNum(total)
                .matchedNum(matched)
                .unMatcheNum(Math.max(0, total - matched))
                .rejectNum(rejected)
                .lockedNum(locked)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reset(SourceFileResetCmd command) {
        List<String> ids = command == null ? Collections.emptyList() : normalizeIds(command.getIds());
        if (ids.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "重置异常文件id不能为空");
        }
        for (String transferId : ids) {
            resetOne(transferId);
        }
    }

    @Override
    public ResponseEntity<Resource> download(String fileState, String id) {
        if (!StringUtils.hasText(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "源文件主键不能为空");
        }
        TransferObjectDownloadViewDTO downloadView = transferObjectQueryService.downloadObject(id.trim());
        Resource resource = new FileSystemResource(downloadView.getFilePath());
        MediaType contentType = resolveContentType(downloadView.getContentType());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(downloadView.getFileName(), StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(contentType);
        if (downloadView.getContentLength() != null && downloadView.getContentLength() >= 0) {
            builder.contentLength(downloadView.getContentLength());
        }
        return builder.body(resource);
    }

    @Override
    public ResponseEntity<Resource> exportList(SourceFileManageQuery query) {
        List<SourceFileManageVO> rows = buildRows(query);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "当前查询条件下没有可下载文件");
        }
        if (rows.size() > BATCH_DOWNLOAD_LIMIT) {
            throw new ResponseStatusException(BAD_REQUEST, "批量下载最多支持" + BATCH_DOWNLOAD_LIMIT + "个文件，请缩小查询范围");
        }
        byte[] zipBytes = buildDownloadZip(rows);
        String fileName = "source-files-" + LocalDateTime.now().format(DOWNLOAD_TS) + ".zip";
        ByteArrayResource resource = new ByteArrayResource(zipBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipBytes.length)
                .body(resource);
    }

    private byte[] buildDownloadZip(List<SourceFileManageVO> rows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<String> skipped = new ArrayList<>();
        Map<String, Integer> entryNames = new HashMap<>();
        int downloaded = 0;
        try (ZipOutputStream zipStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (SourceFileManageVO row : rows) {
                if (row == null || !StringUtils.hasText(row.getId())) {
                    skipped.add("未知文件：缺少文件主键");
                    continue;
                }
                try {
                    TransferObjectDownloadViewDTO downloadView = transferObjectQueryService.downloadObject(row.getId().trim());
                    Path filePath = downloadView.getFilePath();
                    String entryName = uniqueZipEntryName(sanitizeZipEntryName(firstNonBlank(downloadView.getFileName(), row.getFileName(), row.getId())), entryNames);
                    zipStream.putNextEntry(new ZipEntry(entryName));
                    Files.copy(filePath, zipStream);
                    zipStream.closeEntry();
                    downloaded++;
                } catch (ResponseStatusException exception) {
                    skipped.add(firstNonBlank(row.getFileName(), row.getId()) + "：" + firstNonBlank(exception.getReason(), "文件不可下载"));
                } catch (Exception exception) {
                    skipped.add(firstNonBlank(row.getFileName(), row.getId()) + "：" + exception.getMessage());
                    log.warn("文件收取管理批量下载跳过文件，id={}, fileName={}", row.getId(), row.getFileName(), exception);
                }
            }
            if (!skipped.isEmpty()) {
                writeDownloadManifest(zipStream, skipped);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("源文件批量下载压缩包生成失败", exception);
        }
        if (downloaded == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "当前查询范围内没有可下载的本地文件");
        }
        return outputStream.toByteArray();
    }

    private void writeDownloadManifest(ZipOutputStream zipStream, List<String> skipped) throws IOException {
        zipStream.putNextEntry(new ZipEntry("download-manifest.txt"));
        String content = "以下文件未能加入压缩包：\n" + String.join("\n", skipped);
        zipStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipStream.closeEntry();
    }

    private void resetOne(String transferId) {
        if (retryParsedBatch(transferId)) {
            retagTransferObject(transferId);
            return;
        }
        ParseQueuePO queue = latestQueue(transferId);
        if (queue != null) {
            ParseQueueRetryCommand retryCommand = new ParseQueueRetryCommand();
            retryCommand.setForceRebuild(Boolean.TRUE);
            parseQueueManagementAppService.retryQueue(queue.getQueueId(), retryCommand);
        } else {
            ParseQueueGenerateCommand generateCommand = new ParseQueueGenerateCommand();
            generateCommand.setTransferId(transferId);
            generateCommand.setBusinessKey("transfer:" + transferId);
            generateCommand.setForceRebuild(Boolean.TRUE);
            parseQueueManagementAppService.generateQueue(generateCommand);
        }
        retagTransferObject(transferId);
    }

    private boolean retryParsedBatch(String transferId) {
        if (!StringUtils.hasText(transferId)) {
            return false;
        }
        try {
            OutsourcedDataTaskActionCommand retryCommand = new OutsourcedDataTaskActionCommand();
            retryCommand.setReason("文件收取管理重新分析");
            outsourcedDataTaskService.retry("FILE-" + transferId.trim(), retryCommand);
            log.info("文件收取管理已按估值解析任务逻辑提交重新解析，transferId={}", transferId);
            return true;
        } catch (IllegalArgumentException exception) {
            log.info("文件收取管理未找到可直接重跑的估值解析批次，回退到解析队列重置，transferId={}, message={}",
                    transferId,
                    exception.getMessage());
            return false;
        }
    }

    private void retagTransferObject(String transferId) {
        TransferObjectRetagCommand retagCommand = new TransferObjectRetagCommand();
        retagCommand.setFingerprint(findObjectById(transferId).getFingerprint());
        transferObjectManagementAppService.retag(retagCommand);
    }

    private List<SourceFileManageVO> buildRows(SourceFileManageQuery query) {
        return buildRows(query, true);
    }

    private List<SourceFileManageVO> buildRows(SourceFileManageQuery query, boolean applyStateGroup) {
        List<TransferObjectPO> objects = queryObjects(query);
        if (objects.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> transferIds = objects.stream()
                .map(TransferObjectPO::getTransferId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        Map<String, List<TransferObjectTagPO>> tagMap = loadTags(transferIds);
        Map<String, ParseQueuePO> queueMap = loadLatestQueues(transferIds);
        Map<String, TransferSourcePO> sourceMap = loadSources(objects);
        return objects.stream()
                .map(object -> toView(object, tagMap.get(object.getTransferId()), queueMap.get(object.getTransferId()), sourceMap.get(object.getSourceId())))
                .filter(row -> matchesViewFilters(row, query, applyStateGroup))
                .filter(row -> applyStateGroup || !STATE_ANALYZE_EXCEPTION.equals(resolveStateCode(row.getFileState())))
                .collect(Collectors.toList());
    }

    private List<TransferObjectPO> queryObjects(SourceFileManageQuery query) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TransferObjectPO> wrapper =
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .like(StringUtils.hasText(text(query == null ? null : query.getFileName())), TransferObjectPO::getOriginalName, text(query == null ? null : query.getFileName()))
                        .eq(StringUtils.hasText(text(query == null ? null : query.getOrgCd())), TransferObjectPO::getSourceCode, text(query == null ? null : query.getOrgCd()))
                        .ge(parseDate(query == null ? null : query.getBizDateStart()) != null, TransferObjectPO::getBusinessDate, parseDate(query == null ? null : query.getBizDateStart()))
                        .le(parseDate(query == null ? null : query.getBizDateEnd()) != null, TransferObjectPO::getBusinessDate, parseDate(query == null ? null : query.getBizDateEnd()))
                        .ge(parseDateTimeStart(query == null ? null : query.getFileReceiveDateStart()) != null, TransferObjectPO::getReceivedAt, parseDateTimeStart(query == null ? null : query.getFileReceiveDateStart()))
                        .lt(parseDateTimeEndExclusive(query == null ? null : query.getFileReceiveDateEnd()) != null, TransferObjectPO::getReceivedAt, parseDateTimeEndExclusive(query == null ? null : query.getFileReceiveDateEnd()))
                        .orderByDesc(TransferObjectPO::getReceivedAt)
                        .orderByDesc(TransferObjectPO::getTransferId);
        return transferObjectRepository.selectList(wrapper);
    }

    private SourceFileManageVO toView(TransferObjectPO object,
                                      List<TransferObjectTagPO> tags,
                                      ParseQueuePO queue,
                                      TransferSourcePO source) {
        String stateCode = resolveStateCode(object, tags, queue);
        return SourceFileManageVO.builder()
                .id(object.getTransferId())
                .fileName(object.getOriginalName())
                .pdName(firstTagValue(tags, "PD_NAME", "PRODUCT_NAME"))
                .pdCd(firstTagValue(tags, "PD_CD", "PRODUCT_CODE", "PD_CODE"))
                .orgName(source == null ? object.getSourceCode() : source.getSourceName())
                .orgCd(object.getSourceCode())
                .fileReceiveTime(object.getReceivedAt())
                .bizDate(object.getBusinessDate())
                .fileReceiveChannel(resolveReceiveChannel(object))
                .fileType(resolveFileType(object))
                .pdType(firstTagValue(tags, "PD_TYPE", "PRODUCT_TYPE"))
                .lastAnalysisTime(resolveLastAnalysisTime(queue))
                .creater("system")
                .fileState(STATE_NAMES.get(stateCode))
                .exceptionMsg(resolveExceptionMessage(object, queue))
                .exceptionCode(stateCode.equals(STATE_ANALYZE_EXCEPTION) ? "PARSE_FAILED" : null)
                .fileAddress(StringUtils.hasText(object.getRealStoragePath()) ? object.getRealStoragePath() : object.getLocalTempPath())
                .build();
    }

    private boolean matchesViewFilters(SourceFileManageVO row, SourceFileManageQuery query, boolean applyStateGroup) {
        if (query == null) {
            return true;
        }
        if (applyStateGroup && StringUtils.hasText(query.getStateGroup()) && !resolveStateCode(row.getFileState()).equals(query.getStateGroup().trim())) {
            return false;
        }
        if (StringUtils.hasText(query.getFileState()) && !containsAny(query.getFileState(), row.getFileState(), resolveStateCode(row.getFileState()))) {
            return false;
        }
        if (StringUtils.hasText(query.getPdCd()) && !contains(row.getPdCd(), query.getPdCd())) {
            return false;
        }
        if (StringUtils.hasText(query.getPdType()) && !containsAny(query.getPdType(), row.getPdType())) {
            return false;
        }
        if (StringUtils.hasText(query.getFileType()) && !contains(row.getFileType(), query.getFileType())) {
            return false;
        }
        if (StringUtils.hasText(query.getFileReceiveChannel()) && !containsAny(query.getFileReceiveChannel(), row.getFileReceiveChannel())) {
            return false;
        }
        return true;
    }

    private Map<String, List<TransferObjectTagPO>> loadTags(List<String> transferIds) {
        if (transferIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return transferObjectTagRepository.selectList(
                Wrappers.lambdaQuery(TransferObjectTagPO.class)
                        .in(TransferObjectTagPO::getTransferId, transferIds)
                        .orderByAsc(TransferObjectTagPO::getCreatedAt)
        ).stream().collect(Collectors.groupingBy(TransferObjectTagPO::getTransferId));
    }

    private Map<String, ParseQueuePO> loadLatestQueues(List<String> transferIds) {
        if (transferIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ParseQueuePO> result = new HashMap<>();
        parseQueueRepository.selectList(
                Wrappers.lambdaQuery(ParseQueuePO.class)
                        .in(ParseQueuePO::getTransferId, transferIds)
                        .orderByDesc(ParseQueuePO::getCreatedAt)
                        .orderByDesc(ParseQueuePO::getQueueId)
        ).forEach(queue -> result.putIfAbsent(queue.getTransferId(), queue));
        return result;
    }

    private Map<String, TransferSourcePO> loadSources(List<TransferObjectPO> objects) {
        Set<String> sourceIds = objects.stream()
                .map(TransferObjectPO::getSourceId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return transferSourceRepository.selectList(Wrappers.lambdaQuery(TransferSourcePO.class).in(TransferSourcePO::getSourceId, sourceIds))
                .stream()
                .collect(Collectors.toMap(TransferSourcePO::getSourceId, source -> source, (left, right) -> left));
    }

    private TransferObjectPO findObjectById(String transferId) {
        TransferObjectPO object = transferObjectRepository.selectById(transferId);
        if (object == null) {
            throw new ResponseStatusException(BAD_REQUEST, "未找到源文件主键，id=" + transferId);
        }
        return object;
    }

    private ParseQueuePO latestQueue(String transferId) {
        List<ParseQueuePO> queues = parseQueueRepository.selectList(
                Wrappers.lambdaQuery(ParseQueuePO.class)
                        .eq(ParseQueuePO::getTransferId, transferId)
                        .orderByDesc(ParseQueuePO::getCreatedAt)
                        .orderByDesc(ParseQueuePO::getQueueId)
        );
        return queues.isEmpty() ? null : queues.get(0);
    }

    private String resolveStateCode(TransferObjectPO object, List<TransferObjectTagPO> tags, ParseQueuePO queue) {
        if (object != null && "ARCHIVED".equalsIgnoreCase(object.getStatus())) {
            return STATE_INVALID;
        }
        if (!hasValuationTag(tags)) {
            return STATE_UNMATCHED;
        }
        String parseStatus = queue == null ? null : queue.getParseStatus();
        if ("FAILED".equalsIgnoreCase(parseStatus)) {
            return STATE_ANALYZE_EXCEPTION;
        }
        if ("PARSED".equalsIgnoreCase(parseStatus) || "SUCCESS".equalsIgnoreCase(parseStatus) || "COMPLETED".equalsIgnoreCase(parseStatus)) {
            return STATE_ANALYZED;
        }
        return STATE_PENDING;
    }

    private String resolveStateCode(String fileState) {
        if (!StringUtils.hasText(fileState)) {
            return STATE_PENDING;
        }
        String value = fileState.trim();
        if (STATE_NAMES.containsKey(value)) {
            return value;
        }
        for (Map.Entry<String, String> entry : STATE_NAMES.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return value;
    }

    private boolean hasValuationTag(List<TransferObjectTagPO> tags) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        for (TransferObjectTagPO tag : tags) {
            if ("VALUATION_TABLE".equalsIgnoreCase(tag.getTagCode()) || "BUSINESS_DATE".equalsIgnoreCase(tag.getTagCode())) {
                return true;
            }
        }
        return true;
    }

    private String firstTagValue(List<TransferObjectTagPO> tags, String... tagCodes) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        for (String tagCode : tagCodes) {
            for (TransferObjectTagPO tag : tags) {
                if (tagCode.equalsIgnoreCase(tag.getTagCode()) && StringUtils.hasText(tag.getTagValue())) {
                    return tag.getTagValue();
                }
            }
        }
        return null;
    }

    private String resolveReceiveChannel(TransferObjectPO object) {
        if (!StringUtils.hasText(object.getSourceType())) {
            return object.getSourceCode();
        }
        return object.getSourceType();
    }

    private String resolveFileType(TransferObjectPO object) {
        if (StringUtils.hasText(object.getExtension())) {
            return object.getExtension().startsWith(".") ? object.getExtension() : "." + object.getExtension();
        }
        return object.getMimeType();
    }

    private LocalDateTime resolveLastAnalysisTime(ParseQueuePO queue) {
        if (queue == null) {
            return null;
        }
        return queue.getParsedAt() == null ? queue.getUpdatedAt() : queue.getParsedAt();
    }

    private String resolveExceptionMessage(TransferObjectPO object, ParseQueuePO queue) {
        if (queue != null && StringUtils.hasText(queue.getLastErrorMessage())) {
            return queue.getLastErrorMessage();
        }
        return object == null ? null : object.getErrorMessage();
    }

    private TransferSourcePO resolveManualUploadSource() {
        List<TransferSourcePO> candidates = transferSourceRepository.selectList(
                Wrappers.lambdaQuery(TransferSourcePO.class)
                        .eq(TransferSourcePO::getSourceType, "HTTP")
                        .eq(TransferSourcePO::getEnabled, Boolean.TRUE)
        );
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "未配置已启用的 HTTP 手动上传来源");
        }
        List<TransferSourcePO> marked = candidates.stream()
                .filter(this::isManualUploadSource)
                .collect(Collectors.toList());
        if (marked.size() == 1) {
            return marked.get(0);
        }
        if (marked.size() > 1) {
            throw new ResponseStatusException(BAD_REQUEST, "存在多个默认手动上传 HTTP 来源，请只保留一个默认来源");
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        throw new ResponseStatusException(BAD_REQUEST, "存在多个已启用 HTTP 来源，请在来源元数据中标记默认手动上传来源");
    }

    private boolean isManualUploadSource(TransferSourcePO source) {
        if (hasManualUploadMarker(source.getSourceMetaJson()) || hasManualUploadMarker(source.getConnectionConfigJson())) {
            return true;
        }
        String value = (source.getSourceCode() + " " + source.getSourceName()).toLowerCase(Locale.ROOT);
        return value.contains("manual") || value.contains("upload") || value.contains("手动上传") || value.contains("defaultupload");
    }

    private boolean hasManualUploadMarker(String json) {
        if (!StringUtils.hasText(json)) {
            return false;
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return truthy(values.get("manualUpload"))
                    || truthy(values.get("defaultUpload"))
                    || truthy(values.get("defaultManualUpload"))
                    || truthy(values.get("manualUploadDefault"))
                    || manualUploadValue(values.get("usage"))
                    || manualUploadValue(values.get("purpose"))
                    || manualUploadValue(values.get("sourceRole"));
        } catch (Exception exception) {
            String value = json.toLowerCase(Locale.ROOT);
            return value.contains("manual_upload") || value.contains("manualupload") || value.contains("手动上传");
        }
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text) || "y".equalsIgnoreCase(text);
    }

    private boolean manualUploadValue(Object value) {
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "manual_upload".equals(text)
                || "manual-upload".equals(text)
                || "manualupload".equals(text)
                || "手动上传".equals(text);
    }

    private List<TransferSourcePO> listEnabledSources() {
        return transferSourceRepository.selectList(
                Wrappers.lambdaQuery(TransferSourcePO.class)
                        .eq(TransferSourcePO::getEnabled, Boolean.TRUE)
                        .orderByAsc(TransferSourcePO::getSourceCode)
        );
    }

    private int countReceivedObjects(LocalDate receiveDate, LocalDateTime after) {
        return Math.toIntExact(transferObjectRepository.selectCount(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .ge(TransferObjectPO::getReceivedAt, after)
                        .and(wrapper -> wrapper.eq(TransferObjectPO::getReceiveDate, receiveDate)
                                .or()
                                .ge(TransferObjectPO::getReceivedAt, receiveDate.atStartOfDay())
                                .lt(TransferObjectPO::getReceivedAt, receiveDate.plusDays(1).atStartOfDay()))
        ));
    }

    private int countMatchedObjects(LocalDate receiveDate, LocalDateTime after) {
        List<TransferObjectPO> objects = transferObjectRepository.selectList(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .ge(TransferObjectPO::getReceivedAt, after)
                        .and(wrapper -> wrapper.eq(TransferObjectPO::getReceiveDate, receiveDate)
                                .or()
                                .ge(TransferObjectPO::getReceivedAt, receiveDate.atStartOfDay())
                                .lt(TransferObjectPO::getReceivedAt, receiveDate.plusDays(1).atStartOfDay()))
        );
        if (objects.isEmpty()) {
            return 0;
        }
        Set<String> ids = objects.stream().map(TransferObjectPO::getTransferId).collect(Collectors.toSet());
        return transferObjectTagRepository.selectList(
                Wrappers.lambdaQuery(TransferObjectTagPO.class)
                        .in(TransferObjectTagPO::getTransferId, ids)
        ).stream()
                .map(TransferObjectTagPO::getTransferId)
                .collect(Collectors.toSet())
                .size();
    }

    private LocalDate parseBasicDate(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(BAD_REQUEST, "业务日期不能为空");
        }
        try {
            return LocalDate.parse(value.trim(), BASIC_DATE);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "业务日期格式应为yyyyMMdd");
        }
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            return text.length() == 8 ? LocalDate.parse(text, BASIC_DATE) : LocalDate.parse(text);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDateTime parseDateTimeStart(String value) {
        LocalDate date = parseDate(value);
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime parseDateTimeEndExclusive(String value) {
        LocalDate date = parseDate(value);
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> normalizeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private int safePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex <= 0 ? 1 : pageIndex;
    }

    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 500);
    }

    private String text(String value) {
        return value == null ? null : value.trim();
    }

    private boolean contains(String source, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return StringUtils.hasText(source) && source.contains(expected.trim());
    }

    private boolean containsAny(String expectedCsv, String... values) {
        if (!StringUtils.hasText(expectedCsv)) {
            return true;
        }
        List<String> expected = Arrays.stream(expectedCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            for (String item : expected) {
                if (value.equals(item) || value.contains(item) || item.equals(resolveStateCode(value))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String sanitizeZipEntryName(String fileName) {
        String sanitized = StringUtils.hasText(fileName) ? fileName.trim() : "source-file";
        sanitized = sanitized.replace('\\', '_').replace('/', '_');
        sanitized = sanitized.replaceAll("[\\r\\n\\t\\x00-\\x1F]", "_");
        return StringUtils.hasText(sanitized) ? sanitized : "source-file";
    }

    private String uniqueZipEntryName(String fileName, Map<String, Integer> entryNames) {
        Integer count = entryNames.get(fileName);
        if (count == null) {
            entryNames.put(fileName, 1);
            return fileName;
        }
        int next = count + 1;
        entryNames.put(fileName, next);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex) + "-" + next + fileName.substring(dotIndex);
        }
        return fileName + "-" + next;
    }

    private MediaType resolveContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
