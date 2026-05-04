package com.yss.valset.task.infrastructure.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.config.OutsourcedDataTaskStageCatalog;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskBatchPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskLogPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskStepPO;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskBatchRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskLogRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的估值表解析任务持久化网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class OutsourcedDataTaskGatewayImpl implements OutsourcedDataTaskGateway {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OutsourcedDataTaskBatchRepository batchRepository;

    private final OutsourcedDataTaskStepRepository stepRepository;

    private final OutsourcedDataTaskLogRepository logRepository;

    private final ValsetFileInfoGateway valsetFileInfoGateway;

    private OutsourcedDataTaskStageCatalog stageCatalog = new OutsourcedDataTaskStageCatalog();

    @org.springframework.beans.factory.annotation.Autowired
    public void setStageCatalog(OutsourcedDataTaskStageCatalog stageCatalog) {
        if (stageCatalog != null) {
            this.stageCatalog = stageCatalog;
        }
    }

    @Override
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query) {
        int current = normalizePageIndex(query == null ? null : query.getPageIndex());
        int size = normalizePageSize(query == null ? null : query.getPageSize());
        Page<OutsourcedDataTaskBatchPO> page = batchRepository.selectPage(
                new Page<>(current, size),
                buildBatchQuery(query)
                        .orderByDesc(OutsourcedDataTaskBatchPO::getStartedAt)
                        .orderByDesc(OutsourcedDataTaskBatchPO::getBatchId)
        );
        List<OutsourcedDataTaskBatchDTO> records = page.getRecords() == null
                ? List.of()
                : page.getRecords().stream().map(this::toBatchDTO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query) {
        return batchRepository.selectList(
                        buildBatchQuery(query)
                                .orderByDesc(OutsourcedDataTaskBatchPO::getStartedAt)
                                .orderByDesc(OutsourcedDataTaskBatchPO::getBatchId)
                )
                .stream()
                .map(this::toBatchDTO)
                .toList();
    }

    @Override
    public Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(batchRepository.selectById(batchId))
                .map(this::toBatchDTO);
    }

    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return List.of();
        }
        OutsourcedDataTaskBatchPO batch = batchRepository.selectById(batchId);
        List<OutsourcedDataTaskStepDTO> currentSteps = stepRepository.selectList(
                        Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                                .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                                .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
                                .orderByAsc(OutsourcedDataTaskStepPO::getStage)
                                .orderByAsc(OutsourcedDataTaskStepPO::getRunNo)
                                .orderByAsc(OutsourcedDataTaskStepPO::getStepId)
                )
                .stream()
                .map(this::toStepDTO)
                .sorted((left, right) -> Integer.compare(stageOrder(left.getStage()), stageOrder(right.getStage())))
                .toList();
        if (batch == null) {
            return currentSteps;
        }
        return mergeStepsWithBatch(batch, currentSteps);
    }

    @Override
    public PageResult<OutsourcedDataTaskLogDTO> pageLogs(String batchId, String stage, Integer pageIndex, Integer pageSize) {
        int current = normalizePageIndex(pageIndex);
        int size = normalizePageSize(pageSize);
        Page<OutsourcedDataTaskLogPO> page = logRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(OutsourcedDataTaskLogPO.class)
                        .eq(StringUtils.hasText(batchId), OutsourcedDataTaskLogPO::getBatchId, batchId)
                        .eq(StringUtils.hasText(stage), OutsourcedDataTaskLogPO::getStage, stage)
                        .orderByDesc(OutsourcedDataTaskLogPO::getOccurredAt)
                        .orderByDesc(OutsourcedDataTaskLogPO::getLogId)
        );
        List<OutsourcedDataTaskLogDTO> records = page.getRecords() == null
                ? List.of()
                : page.getRecords().stream().map(this::toLogDTO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordParseLifecycleEvent(ParseLifecycleEvent event) {
        if (event == null || event.getStage() == null) {
            return;
        }
        String batchId = resolveBatchId(event);
        if (!StringUtils.hasText(batchId)) {
            return;
        }
        LocalDateTime occurredAt = toLocalDateTime(event.getOccurredAt());
        OutsourcedDataTaskStage stage = mapStage(event.getStage());
        OutsourcedDataTaskStatus stepStatus = mapStepStatus(event.getStage());
        OutsourcedDataTaskStatus batchStatus = mapBatchStatus(event.getStage());
        upsertBatch(event, batchId, stage, batchStatus, occurredAt);
        if (stage != null) {
            upsertStep(event, batchId, stage, stepStatus, occurredAt);
        }
        if (event.getStage() == ParseLifecycleStage.TASK_STANDARDIZED) {
            upsertStep(event, batchId, OutsourcedDataTaskStage.SUBJECT_RECOGNIZE, OutsourcedDataTaskStatus.SUCCESS, occurredAt);
        }
        refreshBatchAggregation(batchId, occurredAt);
        insertLog(event, batchId, stage, occurredAt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent event) {
        if (event == null || event.getTaskId() == null || stageCatalog.ignoreWorkflowTaskType(event.getTaskType())) {
            return;
        }
        String batchId = resolveBatchId(resolveFileId(event), event.getBusinessKey(), event.getTaskId());
        LocalDateTime occurredAt = toLocalDateTime(event.getOccurredAt());
        OutsourcedDataTaskStage stage = mapWorkflowStage(event);
        OutsourcedDataTaskStatus status = mapWorkflowStatus(event.getTaskStatus());
        upsertWorkflowBatch(event, batchId, stage, status, occurredAt);
        upsertWorkflowStep(event, batchId, stage, status, occurredAt);
        refreshBatchAggregation(batchId, occurredAt);
        insertWorkflowLog(event, batchId, stage, occurredAt);
    }

    private LambdaQueryWrapper<OutsourcedDataTaskBatchPO> buildBatchQuery(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return Wrappers.lambdaQuery(OutsourcedDataTaskBatchPO.class)
                    .and(this::applyCurrentBatchFilter);
        }
        LocalDate taskDate = parseDate(query.getTaskDate());
        LocalDate businessDate = parseDate(query.getBusinessDate());
        LambdaQueryWrapper<OutsourcedDataTaskBatchPO> wrapper = Wrappers.lambdaQuery(OutsourcedDataTaskBatchPO.class)
                .eq(StringUtils.hasText(query.getBatchId()), OutsourcedDataTaskBatchPO::getBatchId, trim(query.getBatchId()))
                .ge(taskDate != null, OutsourcedDataTaskBatchPO::getStartedAt, taskDate != null ? taskDate.atStartOfDay() : null)
                .lt(taskDate != null, OutsourcedDataTaskBatchPO::getStartedAt, taskDate != null ? taskDate.plusDays(1).atStartOfDay() : null)
                .eq(businessDate != null, OutsourcedDataTaskBatchPO::getBusinessDate, businessDate)
                .like(StringUtils.hasText(query.getManagerName()), OutsourcedDataTaskBatchPO::getManagerName, trim(query.getManagerName()))
                .and(StringUtils.hasText(query.getProductKeyword()), criteria -> criteria
                        .like(OutsourcedDataTaskBatchPO::getProductCode, trim(query.getProductKeyword()))
                        .or()
                        .like(OutsourcedDataTaskBatchPO::getProductName, trim(query.getProductKeyword())))
                .eq(StringUtils.hasText(query.getStatus()), OutsourcedDataTaskBatchPO::getStatus, trim(query.getStatus()))
                .eq(StringUtils.hasText(query.getSourceType()), OutsourcedDataTaskBatchPO::getSourceType, trim(query.getSourceType()))
                .and(StringUtils.hasText(query.getErrorType()), criteria -> criteria
                        .like(OutsourcedDataTaskBatchPO::getLastErrorCode, trim(query.getErrorType()))
                        .or()
                        .like(OutsourcedDataTaskBatchPO::getLastErrorMessage, trim(query.getErrorType())));
        if (StringUtils.hasText(query.getStage())) {
            List<String> stageBatchIds = resolveBatchIdsByStage(query.getStage());
            if (stageBatchIds.isEmpty()) {
                return wrapper.eq(OutsourcedDataTaskBatchPO::getBatchId, "__NO_MATCH__");
            }
            wrapper.in(OutsourcedDataTaskBatchPO::getBatchId, stageBatchIds);
        }
        if (!Boolean.TRUE.equals(query.getIncludeHistory())) {
            wrapper.and(this::applyCurrentBatchFilter);
        }
        return wrapper;
    }

    private void applyCurrentBatchFilter(LambdaQueryWrapper<OutsourcedDataTaskBatchPO> wrapper) {
        wrapper.and(criteria -> criteria
                .likeRight(OutsourcedDataTaskBatchPO::getBatchId, "FILE-")
                .or()
                .isNotNull(OutsourcedDataTaskBatchPO::getFileId));
    }

    private OutsourcedDataTaskBatchDTO toBatchDTO(OutsourcedDataTaskBatchPO po) {
        OutsourcedDataTaskBatchDTO dto = new OutsourcedDataTaskBatchDTO();
        dto.setBatchId(po.getBatchId());
        dto.setBatchName(po.getBatchName());
        dto.setBusinessDate(formatDate(po.getBusinessDate()));
        dto.setProductCode(po.getProductCode());
        dto.setProductName(po.getProductName());
        dto.setManagerName(po.getManagerName());
        ValsetFileInfo fileInfo = resolveBatchFileInfo(po);
        dto.setBusinessDate(formatDate(resolveBatchBusinessDate(po, fileInfo)));
        dto.setFileId(firstText(po.getFileId(), fileInfo == null || fileInfo.getFileId() == null ? null : String.valueOf(fileInfo.getFileId())));
        dto.setFilesysFileId(firstText(po.getFilesysFileId(), resolveFilesysFileId(fileInfo)));
        dto.setOriginalFileName(firstText(po.getOriginalFileName(), fileInfo == null ? null : fileInfo.getFileNameOriginal()));
        dto.setSourceType(po.getSourceType());
        OutsourcedDataTaskStage displayStage = displayStage(enumStage(po.getCurrentStage()));
        String displayStageName = displayStage == null ? po.getCurrentStage() : displayStage.getLabel();
        dto.setCurrentStage(displayStage == null ? po.getCurrentStage() : displayStage.name());
        dto.setCurrentStep(displayStage == null ? po.getCurrentStage() : displayStage.name());
        dto.setCurrentStageName(displayStageName);
        dto.setCurrentStepName(displayStageName);
        dto.setStatus(po.getStatus());
        dto.setStatusName(statusLabel(po.getStatus()));
        dto.setProgress(po.getProgress());
        LocalDateTime startedAt = resolveBatchStartedAt(po.getBatchId(), po.getStartedAt());
        LocalDateTime endedAt = resolveBatchEndedAt(po.getBatchId(), po.getEndedAt());
        dto.setStartedAt(formatDateTime(startedAt));
        dto.setEndedAt(formatDateTime(endedAt));
        Long durationMs = durationMs(startedAt, endedAt);
        dto.setDurationMs(durationMs);
        dto.setDurationText(formatDuration(durationMs, po.getStatus()));
        dto.setLastErrorCode(po.getLastErrorCode());
        dto.setLastErrorMessage(po.getLastErrorMessage());
        return dto;
    }

    private LocalDateTime resolveBatchStartedAt(String batchId, LocalDateTime fallbackStartedAt) {
        if (!StringUtils.hasText(batchId)) {
            return fallbackStartedAt;
        }
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
                        .orderByAsc(OutsourcedDataTaskStepPO::getStage)
                        .orderByDesc(OutsourcedDataTaskStepPO::getRunNo)
                        .orderByAsc(OutsourcedDataTaskStepPO::getStartedAt)
        );
        return steps.stream()
                .map(OutsourcedDataTaskStepPO::getStartedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallbackStartedAt);
    }

    private LocalDateTime resolveBatchEndedAt(String batchId, LocalDateTime fallbackEndedAt) {
        if (!StringUtils.hasText(batchId)) {
            return fallbackEndedAt;
        }
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
                        .orderByDesc(OutsourcedDataTaskStepPO::getStage)
                        .orderByDesc(OutsourcedDataTaskStepPO::getRunNo)
                        .orderByDesc(OutsourcedDataTaskStepPO::getEndedAt)
        );
        return steps.stream()
                .map(OutsourcedDataTaskStepPO::getEndedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(fallbackEndedAt);
    }

    private Long resolveBatchDurationMs(List<OutsourcedDataTaskStepPO> steps,
                                        LocalDateTime fallbackStartedAt,
                                        LocalDateTime fallbackEndedAt) {
        return durationMs(
                resolveBatchStartedAt(steps, fallbackStartedAt),
                resolveBatchEndedAt(steps, fallbackEndedAt)
        );
    }

    private static LocalDateTime resolveBatchStartedAt(List<OutsourcedDataTaskStepPO> steps,
                                                       LocalDateTime fallbackStartedAt) {
        if (steps == null || steps.isEmpty()) {
            return fallbackStartedAt;
        }
        return steps.stream()
                .filter(step -> step.getStartedAt() != null)
                .min(Comparator
                        .comparing(OutsourcedDataTaskStepPO::getRunNo, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OutsourcedDataTaskStepPO::getStartedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(OutsourcedDataTaskStepPO::getStartedAt)
                .filter(Objects::nonNull)
                .orElse(fallbackStartedAt);
    }

    private static LocalDateTime resolveBatchEndedAt(List<OutsourcedDataTaskStepPO> steps,
                                                     LocalDateTime fallbackEndedAt) {
        if (steps == null || steps.isEmpty()) {
            return fallbackEndedAt;
        }
        return steps.stream()
                .filter(step -> step.getEndedAt() != null)
                .max(Comparator
                        .comparing(OutsourcedDataTaskStepPO::getRunNo, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OutsourcedDataTaskStepPO::getEndedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(OutsourcedDataTaskStepPO::getEndedAt)
                .filter(Objects::nonNull)
                .orElse(fallbackEndedAt);
    }

    private List<String> resolveBatchIdsByStage(String stage) {
        return stepRepository.selectList(
                        Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                                .select(OutsourcedDataTaskStepPO::getBatchId)
                                .eq(StringUtils.hasText(stage), OutsourcedDataTaskStepPO::getStage, trim(stage))
                ).stream()
                .map(OutsourcedDataTaskStepPO::getBatchId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private void upsertBatch(ParseLifecycleEvent event,
                             String batchId,
                             OutsourcedDataTaskStage stage,
                             OutsourcedDataTaskStatus status,
                             LocalDateTime occurredAt) {
        OutsourcedDataTaskBatchPO po = batchRepository.selectById(batchId);
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskBatchPO();
            po.setBatchId(batchId);
            po.setBatchName(resolveBatchName(event, batchId));
            po.setBusinessDate(occurredAt.toLocalDate());
            po.setCurrentStage(stage == null ? OutsourcedDataTaskStage.FILE_PARSE.name() : stage.name());
            po.setStatus(OutsourcedDataTaskStatus.PENDING.name());
            po.setProgress(0);
            po.setCreatedAt(occurredAt);
        }
        ValsetFileInfo fileInfo = loadFileInfo(event);
        po.setUpdatedAt(occurredAt);
        po.setBusinessDate(resolveBatchBusinessDate(fileInfo, po.getBusinessDate(), occurredAt.toLocalDate()));
        po.setSourceType(firstText(event.getDataSourceType(), event.getSource(), po.getSourceType()));
        Long fileId = resolveFileId(event);
        po.setFileId(fileId == null ? po.getFileId() : String.valueOf(fileId));
        po.setFilesysFileId(firstText(
                attributeText(event.getAttributes(), "filesysFileId"),
                resolveFilesysFileId(fileInfo),
                po.getFilesysFileId()));
        po.setProductCode(firstText(attributeText(event.getAttributes(), "productCode"), po.getProductCode()));
        po.setProductName(firstText(attributeText(event.getAttributes(), "productName"), po.getProductName()));
        po.setManagerName(firstText(attributeText(event.getAttributes(), "managerName"), attributeText(event.getAttributes(), "managerOrg"), po.getManagerName()));
        po.setValuationDate(firstDate(attributeText(event.getAttributes(), "valuationDate"), po.getValuationDate()));
        po.setOriginalFileName(firstText(fileInfo == null ? null : fileInfo.getFileNameOriginal(), po.getOriginalFileName()));
        po.setFileFingerprint(firstText(fileInfo == null ? null : fileInfo.getFileFingerprint(), po.getFileFingerprint()));
        po.setStartedAt(po.getStartedAt() == null ? occurredAt : po.getStartedAt());
        if (insert) {
            batchRepository.insert(po);
        } else {
            batchRepository.updateById(po);
        }
    }

    private void upsertStep(ParseLifecycleEvent event,
                            String batchId,
                            OutsourcedDataTaskStage stage,
                            OutsourcedDataTaskStatus status,
                            LocalDateTime occurredAt) {
        OutsourcedDataTaskStepPO po = findCurrentStep(batchId, stage);
        if (shouldStartNewRun(po, status, event == null ? null : event.getTaskId())) {
            markHistorical(po, occurredAt);
            po = null;
        }
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskStepPO();
            int runNo = nextRunNo(batchId, stage);
            po.setStepId(stepId(batchId, stage, runNo));
            po.setBatchId(batchId);
            po.setStage(stage.name());
            po.setRunNo(runNo);
            po.setCurrentFlag(true);
            po.setCreatedAt(occurredAt);
            po.setStartedAt(occurredAt);
        }
        po.setTaskId(event.getTaskId() == null ? po.getTaskId() : String.valueOf(event.getTaskId()));
        po.setTaskType(firstText(po.getTaskType(), "PARSE_WORKBOOK"));
        po.setTriggerMode(firstText(event.getTriggerMode(), po.getTriggerMode()));
        po.setStatus(status == null ? po.getStatus() : status.name());
        po.setProgress(resolveStepProgress(status));
        po.setInputSummary(firstText(po.getInputSummary(), event.getBusinessKey(), event.getMessage()));
        po.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? event.getMessage() : po.getOutputSummary());
        po.setUpdatedAt(occurredAt);
        if (status == OutsourcedDataTaskStatus.SUCCESS || status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.STOPPED) {
            po.setEndedAt(occurredAt);
            po.setDurationMs(durationMs(po.getStartedAt(), occurredAt));
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            po.setErrorCode(event.getStage().name());
            po.setErrorMessage(firstText(event.getErrorMessage(), attributeText(event.getAttributes(), "errorMessage"), event.getMessage()));
        }
        po.setLogRef("parse-lifecycle:" + event.getEventId());
        if (insert) {
            stepRepository.insert(po);
        } else {
            stepRepository.updateById(po);
        }
    }

    private void insertLog(ParseLifecycleEvent event,
                           String batchId,
                           OutsourcedDataTaskStage stage,
                           LocalDateTime occurredAt) {
        OutsourcedDataTaskLogPO po = new OutsourcedDataTaskLogPO();
        po.setLogId(firstText(event.getEventId(), batchId + "-" + event.getStage().name() + "-" + occurredAt));
        po.setBatchId(batchId);
        po.setStepId(stage == null ? null : currentStepId(batchId, stage));
        po.setStage(stage == null ? null : stage.name());
        po.setLogLevel(stageCatalog.resolveParseStepStatus(event.getStage()) == OutsourcedDataTaskStatus.FAILED ? "ERROR" : "INFO");
        po.setMessage(firstText(event.getMessage(), event.getErrorMessage(), event.getStage().name()));
        po.setOccurredAt(occurredAt);
        po.setCreatedAt(occurredAt);
        if (logRepository.selectById(po.getLogId()) == null) {
            logRepository.insert(po);
        }
    }

    private void upsertWorkflowBatch(WorkflowTaskLifecycleEvent event,
                                     String batchId,
                                     OutsourcedDataTaskStage stage,
                                     OutsourcedDataTaskStatus status,
                                     LocalDateTime occurredAt) {
        OutsourcedDataTaskBatchPO po = batchRepository.selectById(batchId);
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskBatchPO();
            po.setBatchId(batchId);
            po.setBatchName(firstText(event.getBusinessKey(), batchId));
            po.setBusinessDate(occurredAt.toLocalDate());
            po.setCurrentStage(stage.name());
            po.setStatus(OutsourcedDataTaskStatus.PENDING.name());
            po.setProgress(0);
            po.setCreatedAt(occurredAt);
        }
        ValsetFileInfo fileInfo = loadFileInfo(resolveFileId(event));
        po.setUpdatedAt(occurredAt);
        po.setBusinessDate(resolveBatchBusinessDate(fileInfo, po.getBusinessDate(), occurredAt.toLocalDate()));
        Long fileId = resolveFileId(event);
        po.setFileId(fileId == null ? po.getFileId() : String.valueOf(fileId));
        po.setFilesysFileId(firstText(attributeText(event.getAttributes(), "filesysFileId"), resolveFilesysFileId(fileInfo), po.getFilesysFileId()));
        po.setProductCode(firstText(attributeText(event.getAttributes(), "productCode"), po.getProductCode()));
        po.setProductName(firstText(attributeText(event.getAttributes(), "productName"), po.getProductName()));
        po.setManagerName(firstText(attributeText(event.getAttributes(), "managerName"), attributeText(event.getAttributes(), "managerOrg"), po.getManagerName()));
        po.setValuationDate(firstDate(attributeText(event.getAttributes(), "valuationDate"), po.getValuationDate()));
        po.setOriginalFileName(firstText(fileInfo == null ? null : fileInfo.getFileNameOriginal(), po.getOriginalFileName()));
        po.setFileFingerprint(firstText(fileInfo == null ? null : fileInfo.getFileFingerprint(), po.getFileFingerprint()));
        po.setSourceType(firstText(attributeText(event.getAttributes(), "dataSourceType"), attributeText(event.getAttributes(), "sourceType"), event.getTaskType() == null ? null : event.getTaskType().name(), po.getSourceType()));
        po.setStartedAt(po.getStartedAt() == null ? occurredAt : po.getStartedAt());
        if (insert) {
            batchRepository.insert(po);
        } else {
            batchRepository.updateById(po);
        }
    }

    private void upsertWorkflowStep(WorkflowTaskLifecycleEvent event,
                                    String batchId,
                                    OutsourcedDataTaskStage stage,
                                    OutsourcedDataTaskStatus status,
                                    LocalDateTime occurredAt) {
        OutsourcedDataTaskStepPO po = findCurrentStep(batchId, stage);
        if (shouldStartNewRun(po, status, event == null ? null : event.getTaskId())) {
            markHistorical(po, occurredAt);
            po = null;
        }
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskStepPO();
            int runNo = nextRunNo(batchId, stage);
            po.setStepId(stepId(batchId, stage, runNo));
            po.setBatchId(batchId);
            po.setStage(stage.name());
            po.setRunNo(runNo);
            po.setCurrentFlag(true);
            po.setStartedAt(occurredAt);
            po.setCreatedAt(occurredAt);
        }
        po.setTaskId(String.valueOf(event.getTaskId()));
        po.setTaskType(event.getTaskType() == null ? po.getTaskType() : event.getTaskType().name());
        po.setTriggerMode("DEPENDENCY");
        po.setStatus(status.name());
        po.setProgress(resolveStepProgress(status));
        po.setInputSummary(firstText(po.getInputSummary(), event.getInputSummary(), event.getBusinessKey(), event.getMessage()));
        po.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? firstText(event.getOutputSummary(), event.getMessage()) : po.getOutputSummary());
        po.setUpdatedAt(occurredAt);
        if (status == OutsourcedDataTaskStatus.SUCCESS || status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.STOPPED) {
            po.setEndedAt(occurredAt);
            po.setDurationMs(durationMs(po.getStartedAt(), occurredAt));
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            po.setErrorCode(firstText(event.getErrorCode(), event.getTaskStatus() == null ? null : event.getTaskStatus().name()));
            po.setErrorMessage(firstText(event.getErrorMessage(), event.getMessage()));
        }
        po.setLogRef("workflow-task:" + event.getEventId());
        if (insert) {
            stepRepository.insert(po);
        } else {
            stepRepository.updateById(po);
        }
    }

    private void insertWorkflowLog(WorkflowTaskLifecycleEvent event,
                                   String batchId,
                                   OutsourcedDataTaskStage stage,
                                   LocalDateTime occurredAt) {
        OutsourcedDataTaskLogPO po = new OutsourcedDataTaskLogPO();
        po.setLogId(firstText(event.getEventId(), batchId + "-" + event.getTaskStatus() + "-" + occurredAt));
        po.setBatchId(batchId);
        po.setStepId(currentStepId(batchId, stage));
        po.setStage(stage.name());
        po.setLogLevel(event.getTaskStatus() == TaskStatus.FAILED ? "ERROR" : "INFO");
        po.setMessage(firstText(event.getMessage(), event.getErrorMessage(), event.getTaskStatus() == null ? null : event.getTaskStatus().name()));
        po.setOccurredAt(occurredAt);
        po.setCreatedAt(occurredAt);
        if (logRepository.selectById(po.getLogId()) == null) {
            logRepository.insert(po);
        }
    }

    private OutsourcedDataTaskStepPO findCurrentStep(String batchId, OutsourcedDataTaskStage stage) {
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getStage, stage.name())
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
                        .orderByDesc(OutsourcedDataTaskStepPO::getRunNo)
                        .orderByDesc(OutsourcedDataTaskStepPO::getStepId)
        );
        return steps == null || steps.isEmpty() ? null : steps.get(0);
    }

    private int nextRunNo(String batchId, OutsourcedDataTaskStage stage) {
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getStage, stage.name())
                        .orderByDesc(OutsourcedDataTaskStepPO::getRunNo)
                        .last("limit 1")
        );
        if (steps == null || steps.isEmpty() || steps.get(0).getRunNo() == null) {
            return 1;
        }
        return steps.get(0).getRunNo() + 1;
    }

    private void markHistorical(OutsourcedDataTaskStepPO step, LocalDateTime occurredAt) {
        if (step == null) {
            return;
        }
        step.setCurrentFlag(false);
        step.setUpdatedAt(occurredAt);
        stepRepository.updateById(step);
    }

    private static boolean shouldStartNewRun(OutsourcedDataTaskStepPO step, OutsourcedDataTaskStatus status, Long taskId) {
        if (step == null || status != OutsourcedDataTaskStatus.RUNNING) {
            if (step == null || taskId == null) {
                return false;
            }
            return !Objects.equals(step.getTaskId(), String.valueOf(taskId));
        }
        if (taskId != null && !Objects.equals(step.getTaskId(), String.valueOf(taskId))) {
            return true;
        }
        return isTerminalStatus(step.getStatus());
    }

    private static boolean isTerminalStatus(String status) {
        return OutsourcedDataTaskStatus.SUCCESS.name().equals(status)
                || OutsourcedDataTaskStatus.FAILED.name().equals(status)
                || OutsourcedDataTaskStatus.STOPPED.name().equals(status);
    }

    private String currentStepId(String batchId, OutsourcedDataTaskStage stage) {
        OutsourcedDataTaskStepPO step = findCurrentStep(batchId, stage);
        return step == null ? stepId(batchId, stage, 1) : step.getStepId();
    }

    private static String stepId(String batchId, OutsourcedDataTaskStage stage, int runNo) {
        return batchId + "-" + stage.name() + "-" + runNo;
    }

    private void refreshBatchAggregation(String batchId, LocalDateTime occurredAt) {
        OutsourcedDataTaskBatchPO batch = batchRepository.selectById(batchId);
        if (batch == null) {
            return;
        }
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
        );
        if (steps == null || steps.isEmpty()) {
            return;
        }
        reconcileSequentialStepStates(steps, occurredAt);
        OutsourcedDataTaskStepPO currentStep = resolveCurrentStageStep(steps);
        String status = aggregateBatchStatus(steps);
        batch.setCurrentStage(currentStep == null ? batch.getCurrentStage() : currentStep.getStage());
        batch.setStatus(status);
        batch.setProgress(resolveBatchProgress(
                currentStep == null ? null : displayStage(enumStage(currentStep.getStage())),
                enumStatus(status)));
        batch.setStartedAt(resolveBatchStartedAt(batchId, batch.getStartedAt()));
        batch.setUpdatedAt(occurredAt);
        if (isTerminalStatus(status)) {
            batch.setEndedAt(occurredAt);
            batch.setDurationMs(resolveBatchDurationMs(steps, batch.getStartedAt(), occurredAt));
        } else {
            batch.setEndedAt(null);
            batch.setDurationMs(null);
        }
        Optional<OutsourcedDataTaskStepPO> errorStep = steps.stream()
                .filter(step -> OutsourcedDataTaskStatus.FAILED.name().equals(step.getStatus())
                        || OutsourcedDataTaskStatus.BLOCKED.name().equals(step.getStatus()))
                .max(Comparator.comparing(OutsourcedDataTaskStepPO::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        batch.setLastErrorCode(errorStep.map(OutsourcedDataTaskStepPO::getErrorCode).orElse(null));
        batch.setLastErrorMessage(errorStep.map(OutsourcedDataTaskStepPO::getErrorMessage).orElse(null));
        batchRepository.updateById(batch);
    }

    private void reconcileSequentialStepStates(List<OutsourcedDataTaskStepPO> steps, LocalDateTime occurredAt) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        List<OutsourcedDataTaskStepPO> orderedSteps = steps.stream()
                .sorted(Comparator.comparing((OutsourcedDataTaskStepPO step) -> stageOrder(step.getStage()))
                        .thenComparing(OutsourcedDataTaskStepPO::getRunNo, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OutsourcedDataTaskStepPO::getStepId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        int latestObservedStageOrder = orderedSteps.stream()
                .map(OutsourcedDataTaskStepPO::getStage)
                .mapToInt(this::stageOrder)
                .max()
                .orElse(-1);
        for (OutsourcedDataTaskStepPO step : orderedSteps) {
            if (step == null || step.getStage() == null) {
                continue;
            }
            int stageOrder = stageOrder(step.getStage());
            if (stageOrder >= latestObservedStageOrder) {
                continue;
            }
            if (isTerminalStatus(step.getStatus())) {
                continue;
            }
            step.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
            step.setProgress(100);
            if (step.getEndedAt() == null) {
                step.setEndedAt(occurredAt);
            }
            if (step.getDurationMs() == null) {
                step.setDurationMs(durationMs(step.getStartedAt(), occurredAt));
            }
            step.setUpdatedAt(occurredAt);
            stepRepository.updateById(step);
        }
    }

    private OutsourcedDataTaskStepPO resolveCurrentStageStep(List<OutsourcedDataTaskStepPO> steps) {
        Optional<OutsourcedDataTaskStepPO> active = steps.stream()
                .filter(step -> OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus())
                        || OutsourcedDataTaskStatus.FAILED.name().equals(step.getStatus())
                        || OutsourcedDataTaskStatus.BLOCKED.name().equals(step.getStatus()))
                .max(Comparator
                        .comparing(OutsourcedDataTaskStepPO::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(step -> stageOrder(step.getStage())));
        if (active.isPresent()) {
            return active.get();
        }
        return steps.stream()
                .max(Comparator.comparing(step -> stageOrder(step.getStage())))
                .orElse(null);
    }

    private static String aggregateBatchStatus(List<OutsourcedDataTaskStepPO> steps) {
        if (steps.stream().anyMatch(step -> OutsourcedDataTaskStatus.FAILED.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.FAILED.name();
        }
        if (steps.stream().anyMatch(step -> OutsourcedDataTaskStatus.BLOCKED.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.BLOCKED.name();
        }
        if (steps.stream().anyMatch(step -> OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.RUNNING.name();
        }
        if (steps.stream().allMatch(step -> OutsourcedDataTaskStatus.STOPPED.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.STOPPED.name();
        }
        if (steps.stream().allMatch(step -> OutsourcedDataTaskStatus.SUCCESS.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.SUCCESS.name();
        }
        return OutsourcedDataTaskStatus.PENDING.name();
    }

    private OutsourcedDataTaskStepDTO toStepDTO(OutsourcedDataTaskStepPO po) {
        OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
        dto.setStepId(po.getStepId());
        dto.setBatchId(po.getBatchId());
        dto.setStage(po.getStage());
        dto.setStep(po.getStage());
        dto.setStageName(stageLabel(po.getStage()));
        dto.setStepName(stageLabel(po.getStage()));
        dto.setTaskId(po.getTaskId());
        dto.setTaskType(po.getTaskType());
        dto.setRunNo(po.getRunNo());
        dto.setCurrentFlag(po.getCurrentFlag());
        dto.setTriggerMode(po.getTriggerMode());
        dto.setTriggerModeName(triggerModeLabel(po.getTriggerMode()));
        dto.setStatus(po.getStatus());
        dto.setStatusName(statusLabel(po.getStatus()));
        dto.setProgress(po.getProgress());
        dto.setStartedAt(formatDateTime(po.getStartedAt()));
        dto.setEndedAt(formatDateTime(po.getEndedAt()));
        dto.setDurationMs(po.getDurationMs());
        dto.setDurationText(formatDuration(po.getDurationMs(), po.getStatus()));
        dto.setInputSummary(po.getInputSummary());
        dto.setOutputSummary(po.getOutputSummary());
        dto.setErrorCode(po.getErrorCode());
        dto.setErrorMessage(po.getErrorMessage());
        dto.setLogRef(po.getLogRef());
        return dto;
    }

    private OutsourcedDataTaskLogDTO toLogDTO(OutsourcedDataTaskLogPO po) {
        OutsourcedDataTaskLogDTO dto = new OutsourcedDataTaskLogDTO();
        dto.setLogId(po.getLogId());
        dto.setBatchId(po.getBatchId());
        dto.setStepId(po.getStepId());
        dto.setStage(po.getStage());
        dto.setLogLevel(po.getLogLevel());
        dto.setMessage(po.getMessage());
        dto.setOccurredAt(formatDateTime(po.getOccurredAt()));
        return dto;
    }

    private static int normalizePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex < 1 ? 1 : pageIndex;
    }

    private static int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, 200);
    }

    private static String resolveBatchId(ParseLifecycleEvent event) {
        Long fileId = resolveFileId(event);
        if (fileId != null) {
            return "FILE-" + fileId;
        }
        if (StringUtils.hasText(event.getBusinessKey())) {
            return "BIZ-" + sanitizeBusinessKey(event.getBusinessKey());
        }
        if (StringUtils.hasText(event.getQueueId())) {
            return "QUEUE-" + event.getQueueId();
        }
        if (event.getTaskId() != null) {
            return "TASK-" + event.getTaskId();
        }
        return null;
    }

    private static String resolveBatchId(Long fileId, String businessKey, Long taskId) {
        if (fileId != null) {
            return "FILE-" + fileId;
        }
        if (StringUtils.hasText(businessKey)) {
            return "BIZ-" + sanitizeBusinessKey(businessKey);
        }
        if (taskId != null) {
            return "TASK-" + taskId;
        }
        return null;
    }

    private static String sanitizeBusinessKey(String businessKey) {
        return businessKey.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String resolveBatchName(ParseLifecycleEvent event, String batchId) {
        return firstText(event.getBusinessKey(), event.getMessage(), batchId);
    }

    private ValsetFileInfo loadFileInfo(ParseLifecycleEvent event) {
        return loadFileInfo(resolveFileId(event));
    }

    private ValsetFileInfo loadFileInfo(Long fileId) {
        if (fileId == null || valsetFileInfoGateway == null) {
            return null;
        }
        try {
            return valsetFileInfoGateway.findById(fileId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private OutsourcedDataTaskStage mapStage(ParseLifecycleStage stage) {
        return stageCatalog.resolveParseLifecycleStage(stage);
    }

    private OutsourcedDataTaskStatus mapStepStatus(ParseLifecycleStage stage) {
        return stageCatalog.resolveParseStepStatus(stage);
    }

    private OutsourcedDataTaskStatus mapBatchStatus(ParseLifecycleStage stage) {
        return stageCatalog.resolveParseBatchStatus(stage);
    }

    private OutsourcedDataTaskStage mapWorkflowStage(WorkflowTaskLifecycleEvent event) {
        if (event == null) {
            return stageCatalog.workflowFallbackStage();
        }
        return stageCatalog.resolveWorkflowStage(event.getTaskType(), event.getTaskStage());
    }

    private OutsourcedDataTaskStatus mapWorkflowStatus(TaskStatus status) {
        return stageCatalog.resolveWorkflowStatus(status);
    }

    private static int resolveStepProgress(OutsourcedDataTaskStatus status) {
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            return 100;
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            return 66;
        }
        if (status == OutsourcedDataTaskStatus.RUNNING) {
            return 50;
        }
        return 0;
    }

    private static OutsourcedDataTaskStage enumStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return null;
        }
        try {
            return OutsourcedDataTaskStage.valueOf(stage.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static OutsourcedDataTaskStage displayStage(OutsourcedDataTaskStage stage) {
        if (stage == OutsourcedDataTaskStage.RAW_DATA_EXTRACT) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        return stage;
    }

    private static OutsourcedDataTaskStatus enumStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OutsourcedDataTaskStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        Instant value = instant == null ? Instant.now() : instant;
        return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    private static Long durationMs(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) {
            return null;
        }
        return java.time.Duration.between(startedAt, endedAt).toMillis();
    }

    private static String attributeText(Map<String, Object> attributes, String key) {
        if (attributes == null || key == null || !attributes.containsKey(key)) {
            return null;
        }
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long resolveFileId(ParseLifecycleEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getFileId() != null) {
            return event.getFileId();
        }
        String fileIdText = attributeText(event.getAttributes(), "fileId");
        if (!StringUtils.hasText(fileIdText)) {
            return null;
        }
        try {
            return Long.parseLong(fileIdText.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long resolveFileId(WorkflowTaskLifecycleEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getFileId() != null) {
            return event.getFileId();
        }
        String fileIdText = attributeText(event.getAttributes(), "fileId");
        if (!StringUtils.hasText(fileIdText)) {
            return null;
        }
        try {
            return Long.parseLong(fileIdText.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String firstText(String... values) {
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

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim(), DATE_FORMATTER);
    }

    private static LocalDate firstDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return parseDate(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String resolveFilesysFileId(ValsetFileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        String sourceMetaValue = jsonText(fileInfo.getSourceMetaJson(), "filesysFileId");
        if (StringUtils.hasText(sourceMetaValue)) {
            return sourceMetaValue;
        }
        String storageMetaValue = jsonText(fileInfo.getStorageMetaJson(), "filesysFileId");
        if (StringUtils.hasText(storageMetaValue)) {
            return storageMetaValue;
        }
        String sourceUri = fileInfo.getSourceUri();
        if (StringUtils.hasText(sourceUri) && sourceUri.trim().startsWith("filesys:")) {
            return sourceUri.trim().substring("filesys:".length());
        }
        return null;
    }

    private static String jsonText(String json, String key) {
        if (!StringUtils.hasText(json) || !StringUtils.hasText(key)) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.isTextual() ? value.asText() : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String stageLabel(String stage) {
        return stageCatalog.stageLabel(stage);
    }

    private String statusLabel(String status) {
        return stageCatalog.statusLabel(status);
    }

    private static String triggerModeLabel(String triggerMode) {
        if ("SCHEDULE".equals(triggerMode)) {
            return "调度执行";
        }
        if ("MANUAL".equals(triggerMode)) {
            return "手动执行";
        }
        if ("DEPENDENCY".equals(triggerMode)) {
            return "依赖触发";
        }
        return triggerMode;
    }

    private static String formatDuration(Long durationMs, String status) {
        if (durationMs == null) {
            return OutsourcedDataTaskStatus.RUNNING.name().equals(status) ? "运行中" : "-";
        }
        long seconds = Math.max(1, durationMs / 1000);
        if (seconds < 60) {
            return seconds + "s";
        }
        return seconds / 60 + "m";
    }

    private int stageOrder(String stage) {
        return stageCatalog.stageOrder(stage);
    }

    private List<OutsourcedDataTaskStepDTO> mergeStepsWithBatch(OutsourcedDataTaskBatchPO batch,
                                                               List<OutsourcedDataTaskStepDTO> currentSteps) {
        Map<String, OutsourcedDataTaskStepDTO> stepsByStage = currentSteps == null
                ? Map.of()
                : currentSteps.stream()
                .filter(step -> StringUtils.hasText(step.getStage()))
                .collect(Collectors.toMap(
                        OutsourcedDataTaskStepDTO::getStage,
                        step -> step,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
        OutsourcedDataTaskStage batchStage = displayStage(enumStage(batch.getCurrentStage()));
        OutsourcedDataTaskStatus batchStatus = enumStatus(batch.getStatus());
        return stageCatalog.stageSequence().stream()
                .map(stage -> {
                    OutsourcedDataTaskStepDTO current = stepsByStage.get(stage.name());
                    if (current != null) {
                        return current;
                    }
                    return buildSyntheticStep(batch, stage, batchStage, batchStatus);
                })
                .sorted((left, right) -> Integer.compare(stageOrder(left.getStage()), stageOrder(right.getStage())))
                .toList();
    }

    private OutsourcedDataTaskStepDTO buildSyntheticStep(OutsourcedDataTaskBatchPO batch,
                                                         OutsourcedDataTaskStage stage,
                                                         OutsourcedDataTaskStage batchStage,
                                                         OutsourcedDataTaskStatus batchStatus) {
        OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
        dto.setStepId(firstText(batch.getBatchId(), "") + "-" + stage.name() + "-1");
        dto.setBatchId(batch.getBatchId());
        dto.setStage(stage.name());
        dto.setStep(stage.name());
        dto.setStageName(stageLabel(stage.name()));
        dto.setStepName(stageLabel(stage.name()));
        dto.setTaskId(null);
        dto.setTaskType(stage.name());
        dto.setRunNo(1);
        OutsourcedDataTaskStage displayBatchStage = stageCatalog.normalizeStage(batch.getCurrentStage());
        dto.setCurrentFlag(displayBatchStage != null && Objects.equals(stage.name(), displayBatchStage.name()));
        boolean beforeCurrent = batchStage != null && stageOrder(stage.name()) < stageOrder(batchStage.name());
        boolean isCurrent = batchStage != null && Objects.equals(stage.name(), batchStage.name());
        OutsourcedDataTaskStatus status;
        if (batchStatus == OutsourcedDataTaskStatus.SUCCESS) {
            status = OutsourcedDataTaskStatus.SUCCESS;
        } else if (batchStatus == OutsourcedDataTaskStatus.FAILED || batchStatus == OutsourcedDataTaskStatus.BLOCKED || batchStatus == OutsourcedDataTaskStatus.STOPPED) {
            status = beforeCurrent ? OutsourcedDataTaskStatus.SUCCESS : (isCurrent ? batchStatus : OutsourcedDataTaskStatus.PENDING);
        } else if (batchStatus == OutsourcedDataTaskStatus.RUNNING) {
            status = beforeCurrent ? OutsourcedDataTaskStatus.SUCCESS : (isCurrent ? OutsourcedDataTaskStatus.RUNNING : OutsourcedDataTaskStatus.PENDING);
        } else {
            status = beforeCurrent ? OutsourcedDataTaskStatus.SUCCESS : (isCurrent ? OutsourcedDataTaskStatus.PENDING : OutsourcedDataTaskStatus.PENDING);
        }
        dto.setStatus(status.name());
        dto.setStatusName(statusLabel(status.name()));
        dto.setProgress(status == OutsourcedDataTaskStatus.SUCCESS
                ? 100
                : status == OutsourcedDataTaskStatus.RUNNING
                ? Math.min(95, resolveBatchProgress(batchStage, batchStatus))
                : 0);
        String startedAt = stage == batchStage
                ? formatDateTime(resolveBatchStartedAt(batch.getBatchId(), batch.getStartedAt()))
                : null;
        dto.setStartedAt(startedAt);
        dto.setEndedAt(status == OutsourcedDataTaskStatus.SUCCESS || status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.STOPPED
                ? formatDateTime(batch.getEndedAt())
                : null);
        Long durationMs = stage == batchStage ? batch.getDurationMs() : null;
        dto.setDurationMs(durationMs);
        dto.setDurationText(stage == batchStage ? formatDuration(batch.getDurationMs(), batch.getStatus()) : "-");
        dto.setInputSummary(stage.getDescription());
        dto.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? stage.getLabel() + "已完成" : null);
        dto.setErrorCode(Objects.equals(stage.name(), batch.getCurrentStage()) ? batch.getLastErrorCode() : null);
        dto.setErrorMessage(Objects.equals(stage.name(), batch.getCurrentStage()) ? batch.getLastErrorMessage() : null);
        dto.setLogRef("batch:" + batch.getBatchId() + ":" + stage.name());
        return dto;
    }

    private int resolveBatchProgress(OutsourcedDataTaskStage stage, OutsourcedDataTaskStatus status) {
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            return 100;
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            return stage == null ? 0 : Math.max(15, stageOrder(stage.name()) * 16);
        }
        return stage == null ? 0 : Math.min(95, stageOrder(stage.name()) * 16 + 10);
    }

    private ValsetFileInfo resolveBatchFileInfo(OutsourcedDataTaskBatchPO batch) {
        if (batch == null || valsetFileInfoGateway == null) {
            return null;
        }
        Long fileId = parseLong(batch.getFileId());
        try {
            if (fileId != null) {
                ValsetFileInfo fileInfo = valsetFileInfoGateway.findById(fileId);
                if (fileInfo != null) {
                    return fileInfo;
                }
            }
            if (StringUtils.hasText(batch.getFileFingerprint())) {
                return valsetFileInfoGateway.findByFingerprint(batch.getFileFingerprint().trim());
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static LocalDate resolveBatchBusinessDate(OutsourcedDataTaskBatchPO batch, ValsetFileInfo fileInfo) {
        if (fileInfo != null && fileInfo.getBusinessDate() != null) {
            return fileInfo.getBusinessDate();
        }
        return batch == null ? null : batch.getBusinessDate();
    }

    private static LocalDate resolveBatchBusinessDate(ValsetFileInfo fileInfo, LocalDate currentValue, LocalDate fallbackValue) {
        if (fileInfo != null && fileInfo.getBusinessDate() != null) {
            return fileInfo.getBusinessDate();
        }
        return currentValue != null ? currentValue : fallbackValue;
    }

    private static Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
