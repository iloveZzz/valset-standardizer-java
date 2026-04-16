package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.application.command.MatchTaskCommand;
import com.yss.subjectmatch.application.port.MatchExecutionUseCase;
import com.yss.subjectmatch.domain.gateway.DwdExternalValuationGateway;
import com.yss.subjectmatch.domain.gateway.MappingHintGateway;
import com.yss.subjectmatch.domain.exporter.ResultExporter;
import com.yss.subjectmatch.domain.gateway.MatchResultGateway;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.gateway.StandardSubjectGateway;
import com.yss.subjectmatch.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.subjectmatch.domain.matcher.SubjectMatcher;

import com.yss.subjectmatch.domain.model.DataSourceType;

import com.yss.subjectmatch.domain.model.*;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import com.yss.subjectmatch.domain.parser.ValuationDataParserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 匹配工作流程实施。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchExecutionAppServiceImpl implements MatchExecutionUseCase {

    private final TaskGateway taskGateway;
    private final ValuationDataParserProvider parserProvider;
    private final StandardSubjectGateway standardSubjectGateway;
    private final MappingHintGateway mappingHintGateway;
    private final SubjectMatcher subjectMatcher;
    private final StandardizedExternalValuationGateway standardizedExternalValuationGateway;
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
    private final MatchResultGateway matchResultGateway;
    private final ObjectMapper objectMapper;

    /**
     * 执行任务 ID 的主题匹配工作流程。
     */
    @Override
    public void execute(Long taskId) {
        try {
            log.info("开始执行主题匹配任务，taskId={}", taskId);
            // 核心步骤 1：查询任务信息并反序列化匹配指令参数
            TaskInfo taskInfo = taskGateway.findById(taskId);
            MatchTaskCommand command = objectMapper.readValue(taskInfo.getInputPayload(), MatchTaskCommand.class);

            long matchStartMs = System.currentTimeMillis();
            // 核心步骤 2：加载并解析外部待匹配的估值表数据源
            ParsedValuationData parsedValuationData = parseWorkbook(command);

            // 核心步骤 3：直接从落地表加载标准参考科目目录
            List<StandardSubject> standardSubjects = loadStandardSubjects();

            // 核心步骤 4：直接从落地表加载历史映射经验库以提升准确率
            MappingHintIndex mappingHintIndex = loadMappingHints();

            // 核心步骤 5：构建匹配上下文，组装运行时所需的映射对象、权重与策略
            MatchContext matchContext = buildMatchContext(parsedValuationData, standardSubjects, mappingHintIndex, command);

            // 核心步骤 6：通过匹配引擎执行打分和候选人选取，产生匹配结果
            List<SubjectMatchResult> results = doMatch(parsedValuationData, matchContext, command);

            // 核心步骤 7：持久化匹配结果以及产出相关数据报表，并更新任务执行状态
            persistResults(taskId, taskInfo.getFileId(), results);
            long matchStandardSubjectTimeMs = System.currentTimeMillis() - matchStartMs;
            taskGateway.updateTaskTimings(taskId, null, null, matchStandardSubjectTimeMs);
            String resultPayload = buildResultPayload( parsedValuationData, standardSubjects, results);
            taskGateway.markSuccess(taskId, resultPayload);
            log.info("科目匹配任务执行完成，taskId={}, subjectCount={}, matchCount={}",
                    taskId,
                    parsedValuationData.getSubjects() == null ? 0 : parsedValuationData.getSubjects().size(),
                    results == null ? 0 : results.size());
        } catch (Exception e) {
            log.error("执行主题匹配任务失败，taskId={}", taskId, e);
            throw new IllegalStateException("Failed to execute match task " + taskId, e);
        }
    }

    /**
     * 解析评估数据源。
     */
    private ParsedValuationData parseWorkbook(MatchTaskCommand command) {
        String sourceTypeStr = command.getDataSourceType();
        DataSourceType type = DataSourceType.EXCEL;
        if (sourceTypeStr != null && !sourceTypeStr.isBlank()) {
            type = DataSourceType.valueOf(sourceTypeStr.toUpperCase());
        }

        if ((type == DataSourceType.EXCEL || type == DataSourceType.CSV) && command.getFileId() != null) {
            ParsedValuationData standardizedSnapshot = standardizedExternalValuationGateway.findLatestByFileId(command.getFileId());
            if (standardizedSnapshot != null) {
                log.info("匹配任务优先使用标准化落地数据，fileId={}", command.getFileId());
                return standardizedSnapshot;
            }
            ParsedValuationData dwdSnapshot = dwdExternalValuationGateway.findLatestByFileId(command.getFileId());
            if (dwdSnapshot != null) {
                log.info("匹配任务未找到标准化落地数据，回退使用 DWD 外部估值标准数据，fileId={}", command.getFileId());
                return dwdSnapshot;
            }
            log.warn("匹配任务未找到标准化落地数据，回退到解析器分析，fileId={}", command.getFileId());
        }

        DataSourceConfig config = buildAnalysisConfig(type, command.getWorkbookPath(), command.getFileId());
                
        return parserProvider.getParser(type).parse(config);
    }

    /**
     * 加载标准主题目录。
     */
    private List<StandardSubject> loadStandardSubjects() {
        List<StandardSubject> standardSubjects = standardSubjectGateway.findAll();
        log.info("从标准科目落地表加载完成，count={}", standardSubjects == null ? 0 : standardSubjects.size());
        return standardSubjects;
    }

    /**
     * 从落地表加载历史映射提示并构建索引。
     */
    private MappingHintIndex loadMappingHints() {
        List<MappingHint> mappingHints = mappingHintGateway.findAll();
        MappingHintIndex mappingHintIndex = MappingHintIndex.fromHints(mappingHints);
        log.info("从历史映射落地表加载完成，count={}", mappingHints == null ? 0 : mappingHints.size());
        return mappingHintIndex;
    }

    /**
     * 构建匹配器运行时上下文。
     */
    private MatchContext buildMatchContext(
            ParsedValuationData parsedValuationData,
            List<StandardSubject> standardSubjects,
            MappingHintIndex mappingHintIndex,
            MatchTaskCommand command
    ) {
        return MatchContext.builder()
                .parsedValuationData(parsedValuationData)
                .standardSubjects(standardSubjects)
                .mappingHintIndex(mappingHintIndex)
                .weights(defaultWeights())
                .runtimeConfig(new MatchRuntimeConfig())
                .build();
    }

    /**
     * 运行匹配器并收集前 K 个结果。
     */
    private List<SubjectMatchResult> doMatch(
            ParsedValuationData parsedValuationData,
            MatchContext matchContext,
            MatchTaskCommand command
    ) {
        int topK = command.getTopK() == null ? 5 : command.getTopK();
        return subjectMatcher.matchSubjects(parsedValuationData.getSubjects(), matchContext, topK);
    }

    /**
     * 持久化匹配结果。
     */
    private void persistResults(Long taskId, Long fileId, List<SubjectMatchResult> results) {
        matchResultGateway.saveResults(taskId, fileId, results);
    }

    /**
     * 为匹配任务构建结构化结果负载。
     */
    private String buildResultPayload(
            ParsedValuationData parsedValuationData,
            List<StandardSubject> standardSubjects,
            List<SubjectMatchResult> results
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workbookPath", parsedValuationData.getWorkbookPath());
            payload.put("sheetName", parsedValuationData.getSheetName());
            payload.put("standardSubjectCount", standardSubjects == null ? 0 : standardSubjects.size());
            payload.put("matchedSubjectCount", results == null ? 0 : results.size());
            payload.put("highConfidenceCount", confidenceCount(results, ConfidenceLevel.HIGH));
            payload.put("mediumConfidenceCount", confidenceCount(results, ConfidenceLevel.MEDIUM));
            payload.put("lowConfidenceCount", confidenceCount(results, ConfidenceLevel.LOW));
            payload.put("reviewQueueCount", results == null ? 0 : (int) results.stream().filter(result -> Boolean.TRUE.equals(result.getNeedsReview())).count());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return String.format(
                    "Parsed=%s, standards=%d, matched=%d",
                    parsedValuationData.getWorkbookPath(),
                    standardSubjects == null ? 0 : standardSubjects.size(),
                    results == null ? 0 : results.size()
            );
        }
    }

    /**
     * 计算具有给定置信水平的结果。
     */
    private int confidenceCount(List<SubjectMatchResult> results, ConfidenceLevel level) {
        if (results == null || level == null) {
            return 0;
        }
        return (int) results.stream()
                .filter(result -> level.equals(result.getConfidenceLevel()))
                .count();
    }

    private DataSourceConfig buildAnalysisConfig(DataSourceType type, String sourceUri, Long fileId) {
        if ((type == DataSourceType.EXCEL || type == DataSourceType.CSV) && fileId == null) {
            throw new IllegalStateException("Excel/CSV 解析需要先完成原始数据提取，并传入 fileId");
        }
        return DataSourceConfig.builder()
                .sourceType(type)
                .sourceUri(sourceUri)
                .additionalParams(fileId == null ? null : String.valueOf(fileId))
                .build();
    }

    /**
     * 返回默认的纯规则权重。
     */
    private MatchWeights defaultWeights() {
        return MatchWeights.builder()
                .nameWeight(BigDecimal.valueOf(0.22))
                .pathWeight(BigDecimal.valueOf(0.22))
                .keywordWeight(BigDecimal.valueOf(0.22))
                .codeWeight(BigDecimal.valueOf(0.04))
                .historyWeight(BigDecimal.valueOf(0.30))
                .embeddingWeight(BigDecimal.ZERO)
                .build();
    }
}
