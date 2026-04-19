package com.yss.valset.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.application.port.EvaluateMappingExecutionUseCase;
import com.yss.valset.domain.exporter.ResultExporter;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.knowledge.MappingSampleLoader;
import com.yss.valset.domain.matcher.ValsetMatcher;
import com.yss.valset.domain.model.*;
import com.yss.valset.extract.support.MappingEvaluationSupport;
import com.yss.valset.knowledge.StandardSubjectLoaderRegistry;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 离线评估工作流程实施。
 */
@Service
public class EvaluateMappingExecutionAppServiceImpl implements EvaluateMappingExecutionUseCase {

    private final TaskGateway taskGateway;
    private final MappingSampleLoader mappingSampleLoader;
    private final StandardSubjectLoaderRegistry standardSubjectLoaderRegistry;
    private final ValsetMatcher subjectMatcher;
    private final ResultExporter resultExporter;
    private final ObjectMapper objectMapper;
    private final String outputRoot;

    public EvaluateMappingExecutionAppServiceImpl(
            TaskGateway taskGateway,
            MappingSampleLoader mappingSampleLoader,
            StandardSubjectLoaderRegistry standardSubjectLoaderRegistry,
            ValsetMatcher subjectMatcher,
            ResultExporter resultExporter,
            ObjectMapper objectMapper,
            @Value("${subject.match.output-dir:output}") String outputRoot
    ) {
        this.taskGateway = taskGateway;
        this.mappingSampleLoader = mappingSampleLoader;
        this.standardSubjectLoaderRegistry = standardSubjectLoaderRegistry;
        this.subjectMatcher = subjectMatcher;
        this.resultExporter = resultExporter;
        this.objectMapper = objectMapper;
        this.outputRoot = outputRoot;
    }

    /**
     * 执行任务 ID 的映射评估工作流。
     */
    @Override
    public void execute(Long taskId) {
        try {
            TaskInfo taskInfo = taskGateway.findById(taskId);
            EvaluateMappingTaskCommand command = objectMapper.readValue(taskInfo.getInputPayload(), EvaluateMappingTaskCommand.class);

            List<MappingSample> samples = MappingEvaluationSupport.deduplicateSamples(
                    mappingSampleLoader.load()
            );
            
            String sourceTypeStr = command.getStandardSourceType();
            DataSourceType type = DataSourceType.EXCEL;
            if (sourceTypeStr != null && !sourceTypeStr.isBlank()) {
                type = DataSourceType.valueOf(sourceTypeStr.toUpperCase());
            }
            DataSourceConfig config = DataSourceConfig.builder()
                    .sourceType(type)
                    .sourceUri(command.getStandardWorkbookPath())
                    .build();
            
            List<StandardSubject> standardSubjects = standardSubjectLoaderRegistry.getLoader(type).load(config);
            
            MappingEvaluationSupport.SplitResult splitResult = MappingEvaluationSupport.splitSamples(samples, splitMode(command));

            List<MappingSample> trainSamples = splitResult.trainSamples();
            List<MappingSample> testSamples = splitResult.testSamples();
            if (command.getMaxTestSamples() != null) {
                testSamples = MappingEvaluationSupport.buildTuningSubset(
                        testSamples,
                        command.getMaxTestSamples(),
                        command.getMaxTestSamples()
                );
            }
            List<MappingSample> tuningSamples = MappingEvaluationSupport.buildTuningSubset(
                    testSamples,
                    20,
                    command.getMaxTuningSamples() == null ? 1500 : command.getMaxTuningSamples()
            );

            MappingHintIndex mappingHintIndex = MappingEvaluationSupport.buildMappingHintIndex(trainSamples);
            MatchRuntimeConfig runtimeConfig = new MatchRuntimeConfig();
            MatchWeights baselineWeights = MappingEvaluationSupport.defaultWeights();
            Map<String, Object> baselineMetrics = MappingEvaluationSupport.runMappingEvaluation(
                    trainSamples,
                    testSamples,
                    standardSubjects,
                    baselineWeights,
                    topK(command),
                    mappingHintIndex,
                    subjectMatcher,
                    runtimeConfig
            );
            MappingEvaluationSupport.SearchResult searchResult = MappingEvaluationSupport.searchMatchWeights(
                    trainSamples,
                    tuningSamples,
                    standardSubjects,
                    topK(command),
                    mappingHintIndex,
                    subjectMatcher,
                    runtimeConfig
            );
            Map<String, Object> recommendedMetrics = MappingEvaluationSupport.runMappingEvaluation(
                    trainSamples,
                    testSamples,
                    standardSubjects,
                    searchResult.bestWeights(),
                    topK(command),
                    mappingHintIndex,
                    subjectMatcher,
                    runtimeConfig
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> failureSamples = (List<Map<String, Object>>) recommendedMetrics.get("failure_samples");
            Map<String, Object> evaluationResult = new LinkedHashMap<>();
            evaluationResult.put("mapping_workbook", command.getMappingWorkbookPath());
            evaluationResult.put("standard_workbook", command.getStandardWorkbookPath());
            evaluationResult.put("split_mode", splitMode(command));
            evaluationResult.put("train_sample_count", trainSamples.size());
            evaluationResult.put("test_sample_count", testSamples.size());
            evaluationResult.put("tuning_sample_count", tuningSamples.size());
            evaluationResult.put("max_tuning_samples", command.getMaxTuningSamples());
            evaluationResult.put("max_test_samples", command.getMaxTestSamples());
            evaluationResult.put("baseline_weights", toMap(baselineWeights));
            evaluationResult.put("baseline_metrics", baselineMetrics);
            evaluationResult.put("recommended_weights", toMap(searchResult.bestWeights()));
            evaluationResult.put("recommended_metrics", recommendedMetrics);
            evaluationResult.put("weight_search", searchResult.report());
            evaluationResult.put("failure_analysis", MappingEvaluationSupport.buildFailureAnalysis(failureSamples));

            resultExporter.exportMappingEvaluation(taskId, evaluationResult);
            String resultPayload = buildResultPayload(
                    taskId,
                    command,
                    trainSamples,
                    testSamples,
                    tuningSamples,
                    baselineMetrics,
                    recommendedMetrics
            );
            taskGateway.markSuccess(taskId, resultPayload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to execute mapping evaluation task " + taskId, exception);
        }
    }

    /**
     * 解析top-K进行评估。
     */
    private int topK(EvaluateMappingTaskCommand command) {
        return command.getTopK() == null ? 5 : command.getTopK();
    }

    /**
     * 解决评价分割模式。
     */
    private String splitMode(EvaluateMappingTaskCommand command) {
        return command.getSplitMode() == null || command.getSplitMode().isBlank()
                ? "org_holdout"
                : command.getSplitMode();
    }

    /**
     * 将权重转换为易于报告的地图。
     */
    private Map<String, Object> toMap(MatchWeights weights) {
        return objectMapper.convertValue(weights, Map.class);
    }

    /**
     * 为评估任务构建结构化结果有效负载。
     */
    private String buildResultPayload(
            Long taskId,
            EvaluateMappingTaskCommand command,
            List<MappingSample> trainSamples,
            List<MappingSample> testSamples,
            List<MappingSample> tuningSamples,
            Map<String, Object> baselineMetrics,
            Map<String, Object> recommendedMetrics
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("mappingWorkbookPath", command.getMappingWorkbookPath());
            payload.put("standardWorkbookPath", command.getStandardWorkbookPath());
            payload.put("splitMode", splitMode(command));
            payload.put("trainSampleCount", trainSamples == null ? 0 : trainSamples.size());
            payload.put("testSampleCount", testSamples == null ? 0 : testSamples.size());
            payload.put("tuningSampleCount", tuningSamples == null ? 0 : tuningSamples.size());
            payload.put("baselineTop1", baselineMetrics.get("top1_accuracy"));
            payload.put("recommendedTop1", recommendedMetrics.get("top1_accuracy"));
            payload.put("baselineTop3", baselineMetrics.get("top3_recall"));
            payload.put("recommendedTop3", recommendedMetrics.get("top3_recall"));
            payload.put("outputDir", resolveTaskOutputDirectory(taskId).toString());
            payload.put("artifacts", List.of(
                    "mapping_evaluation.json",
                    "failure_cluster.json",
                    "weight_search_report.json",
                    "evaluation.duckdb"
            ));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return String.format(
                    "Top1=%s -> %s, Top3=%s -> %s",
                    baselineMetrics.get("top1_accuracy"),
                    recommendedMetrics.get("top1_accuracy"),
                    baselineMetrics.get("top3_recall"),
                    recommendedMetrics.get("top3_recall")
            );
        }
    }

    /**
     * 解析任务输出目录。
     */
    private Path resolveTaskOutputDirectory(Long taskId) {
        return Path.of(outputRoot).toAbsolutePath().resolve("task-" + taskId);
    }
}
