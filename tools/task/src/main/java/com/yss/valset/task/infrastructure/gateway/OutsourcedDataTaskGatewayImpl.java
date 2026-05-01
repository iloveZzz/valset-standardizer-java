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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MyBatis 支持的委外数据任务持久化网关。
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
        return stepRepository.selectList(
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
        if (event == null || event.getTaskId() == null || event.getTaskType() == TaskType.PARSE_WORKBOOK) {
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
            return Wrappers.lambdaQuery(OutsourcedDataTaskBatchPO.class);
        }
        LocalDate businessDate = parseDate(query.getBusinessDate());
        return Wrappers.lambdaQuery(OutsourcedDataTaskBatchPO.class)
                .eq(businessDate != null, OutsourcedDataTaskBatchPO::getBusinessDate, businessDate)
                .like(StringUtils.hasText(query.getManagerName()), OutsourcedDataTaskBatchPO::getManagerName, trim(query.getManagerName()))
                .and(StringUtils.hasText(query.getProductKeyword()), wrapper -> wrapper
                        .like(OutsourcedDataTaskBatchPO::getProductCode, trim(query.getProductKeyword()))
                        .or()
                        .like(OutsourcedDataTaskBatchPO::getProductName, trim(query.getProductKeyword())))
                .eq(StringUtils.hasText(query.getStage()), OutsourcedDataTaskBatchPO::getCurrentStage, trim(query.getStage()))
                .eq(StringUtils.hasText(query.getStatus()), OutsourcedDataTaskBatchPO::getStatus, trim(query.getStatus()))
                .eq(StringUtils.hasText(query.getSourceType()), OutsourcedDataTaskBatchPO::getSourceType, trim(query.getSourceType()))
                .and(StringUtils.hasText(query.getErrorType()), wrapper -> wrapper
                        .like(OutsourcedDataTaskBatchPO::getLastErrorCode, trim(query.getErrorType()))
                        .or()
                        .like(OutsourcedDataTaskBatchPO::getLastErrorMessage, trim(query.getErrorType())));
    }

    private OutsourcedDataTaskBatchDTO toBatchDTO(OutsourcedDataTaskBatchPO po) {
        OutsourcedDataTaskBatchDTO dto = new OutsourcedDataTaskBatchDTO();
        dto.setBatchId(po.getBatchId());
        dto.setBatchName(po.getBatchName());
        dto.setBusinessDate(formatDate(po.getBusinessDate()));
        dto.setValuationDate(formatDate(po.getValuationDate()));
        dto.setProductCode(po.getProductCode());
        dto.setProductName(po.getProductName());
        dto.setManagerName(po.getManagerName());
        dto.setFileId(po.getFileId());
        dto.setFilesysFileId(po.getFilesysFileId());
        dto.setOriginalFileName(po.getOriginalFileName());
        dto.setSourceType(po.getSourceType());
        dto.setCurrentStage(po.getCurrentStage());
        dto.setCurrentStageName(stageLabel(po.getCurrentStage()));
        dto.setStatus(po.getStatus());
        dto.setStatusName(statusLabel(po.getStatus()));
        dto.setProgress(po.getProgress());
        dto.setStartedAt(formatDateTime(po.getStartedAt()));
        dto.setEndedAt(formatDateTime(po.getEndedAt()));
        dto.setDurationMs(po.getDurationMs());
        dto.setDurationText(formatDuration(po.getDurationMs(), po.getStatus()));
        dto.setLastErrorCode(po.getLastErrorCode());
        dto.setLastErrorMessage(po.getLastErrorMessage());
        return dto;
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
        if (shouldStartNewRun(po, status)) {
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
        po.setLogLevel(isFailedLifecycleStage(event.getStage()) ? "ERROR" : "INFO");
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
        if (shouldStartNewRun(po, status)) {
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

    private static boolean shouldStartNewRun(OutsourcedDataTaskStepPO step, OutsourcedDataTaskStatus status) {
        if (step == null || status != OutsourcedDataTaskStatus.RUNNING) {
            return false;
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
        OutsourcedDataTaskStepPO currentStep = resolveCurrentStageStep(steps);
        String status = aggregateBatchStatus(steps);
        batch.setCurrentStage(currentStep == null ? batch.getCurrentStage() : currentStep.getStage());
        batch.setStatus(status);
        batch.setProgress(resolveBatchProgress(
                currentStep == null ? null : enumStage(currentStep.getStage()),
                enumStatus(status)));
        batch.setUpdatedAt(occurredAt);
        if (isTerminalStatus(status)) {
            batch.setEndedAt(occurredAt);
            batch.setDurationMs(durationMs(batch.getStartedAt(), occurredAt));
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

    private static OutsourcedDataTaskStepPO resolveCurrentStageStep(List<OutsourcedDataTaskStepPO> steps) {
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
        dto.setStageName(stageLabel(po.getStage()));
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

    private static OutsourcedDataTaskStage mapStage(ParseLifecycleStage stage) {
        return switch (stage) {
            case TASK_STANDARDIZED -> OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE;
            case TASK_PERSISTED -> OutsourcedDataTaskStage.STANDARD_LANDING;
            case TASK_SUCCEEDED, QUEUE_COMPLETED -> OutsourcedDataTaskStage.VERIFY_ARCHIVE;
            case CYCLE_STARTED, CYCLE_FINISHED, BATCH_STARTED, BATCH_EMPTY, BATCH_FINISHED -> null;
            default -> OutsourcedDataTaskStage.FILE_PARSE;
        };
    }

    private static OutsourcedDataTaskStatus mapStepStatus(ParseLifecycleStage stage) {
        if (isFailedLifecycleStage(stage)) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        return switch (stage) {
            case TASK_EXECUTION_STARTED, TASK_CREATED, TASK_DISPATCHED, QUEUE_SUBSCRIBED -> OutsourcedDataTaskStatus.RUNNING;
            case TASK_RAW_PARSED, TASK_STANDARDIZED, TASK_PERSISTED, TASK_SUCCEEDED, QUEUE_COMPLETED, TASK_REUSED -> OutsourcedDataTaskStatus.SUCCESS;
            case QUEUE_SKIPPED, QUEUE_SUBSCRIBE_CONFLICT, QUEUE_SUBSCRIBE_SKIPPED -> OutsourcedDataTaskStatus.STOPPED;
            default -> OutsourcedDataTaskStatus.PENDING;
        };
    }

    private static OutsourcedDataTaskStatus mapBatchStatus(ParseLifecycleStage stage) {
        if (isFailedLifecycleStage(stage)) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        return switch (stage) {
            case TASK_SUCCEEDED, QUEUE_COMPLETED -> OutsourcedDataTaskStatus.SUCCESS;
            case QUEUE_SKIPPED, QUEUE_SUBSCRIBE_CONFLICT, QUEUE_SUBSCRIBE_SKIPPED -> OutsourcedDataTaskStatus.STOPPED;
            default -> OutsourcedDataTaskStatus.RUNNING;
        };
    }

    private static OutsourcedDataTaskStage mapWorkflowStage(WorkflowTaskLifecycleEvent event) {
        TaskType taskType = event.getTaskType();
        if (taskType == TaskType.EXTRACT_DATA) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        if (taskType == TaskType.MATCH_SUBJECT) {
            return OutsourcedDataTaskStage.SUBJECT_RECOGNIZE;
        }
        if (taskType == TaskType.EXPORT_RESULT) {
            return OutsourcedDataTaskStage.VERIFY_ARCHIVE;
        }
        TaskStage taskStage = event.getTaskStage();
        if (taskStage == TaskStage.EXTRACT) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        if (taskStage == TaskStage.STANDARDIZE) {
            return OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE;
        }
        if (taskStage == TaskStage.MATCH) {
            return OutsourcedDataTaskStage.SUBJECT_RECOGNIZE;
        }
        return OutsourcedDataTaskStage.DATA_PROCESSING;
    }

    private static OutsourcedDataTaskStatus mapWorkflowStatus(TaskStatus status) {
        if (status == TaskStatus.SUCCESS) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        if (status == TaskStatus.FAILED) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        if (status == TaskStatus.CANCELED) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        if (status == TaskStatus.RUNNING || status == TaskStatus.RETRYING) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        return OutsourcedDataTaskStatus.PENDING;
    }

    private static boolean isFailedLifecycleStage(ParseLifecycleStage stage) {
        return stage == ParseLifecycleStage.TASK_FAILED
                || stage == ParseLifecycleStage.QUEUE_FAILED
                || stage == ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_FAILED;
    }

    private static int resolveBatchProgress(OutsourcedDataTaskStage stage, OutsourcedDataTaskStatus status) {
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            return 100;
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            return stage == null ? 0 : Math.max(15, stageOrder(stage.name()) * 16);
        }
        return stage == null ? 0 : Math.min(95, stageOrder(stage.name()) * 16 + 10);
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

    private static String stageLabel(String stage) {
        return Arrays.stream(OutsourcedDataTaskStage.values())
                .filter(item -> Objects.equals(item.name(), stage))
                .map(OutsourcedDataTaskStage::getLabel)
                .findFirst()
                .orElse(stage);
    }

    private static String statusLabel(String status) {
        return Arrays.stream(OutsourcedDataTaskStatus.values())
                .filter(item -> Objects.equals(item.name(), status))
                .map(OutsourcedDataTaskStatus::getLabel)
                .findFirst()
                .orElse(status);
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

    private static int stageOrder(String stage) {
        OutsourcedDataTaskStage[] stages = OutsourcedDataTaskStage.values();
        for (int i = 0; i < stages.length; i++) {
            if (Objects.equals(stages[i].name(), stage)) {
                return i;
            }
        }
        return stages.length;
    }
}
