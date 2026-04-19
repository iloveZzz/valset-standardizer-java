package com.yss.valset.extract.support;

import com.yss.valset.domain.matcher.ValsetMatcher;
import com.yss.valset.domain.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于离线映射评估、权重搜索和故障分析的实用程序。
 */
public final class MappingEvaluationSupport {

    private static final int FAILURE_SAMPLE_LIMIT = 30;

    private MappingEvaluationSupport() {
    }

    /**
     * 使用组织、代码、名称和标准代码删除重复的映射示例。
     */
    public static List<MappingSample> deduplicateSamples(List<MappingSample> samples) {
        Map<String, MappingSample> deduped = new LinkedHashMap<>();
        if (samples == null) {
            return List.of();
        }
        for (MappingSample sample : samples) {
            String key = String.join("|",
                    safe(sample.getOrgName()),
                    safe(sample.getExternalCode()),
                    safe(sample.getExternalName()),
                    safe(sample.getStandardCode()));
            deduped.put(key, sample);
        }
        return new ArrayList<>(deduped.values());
    }

    /**
     * 将样本分为训练分区和测试分区。
     */
    public static SplitResult splitSamples(List<MappingSample> samples, String splitMode) {
        if ("hash_holdout".equalsIgnoreCase(splitMode)) {
            List<MappingSample> train = samples.stream()
                    .filter(sample -> stableBucket(sample.getOrgName(), sample.getExternalCode(), sample.getExternalName()) != 0)
                    .toList();
            List<MappingSample> test = samples.stream()
                    .filter(sample -> stableBucket(sample.getOrgName(), sample.getExternalCode(), sample.getExternalName()) == 0)
                    .toList();
            validateSplit(train, test);
            return new SplitResult(train, test);
        }
        if (!"org_holdout".equalsIgnoreCase(splitMode)) {
            throw new IllegalArgumentException("Unsupported splitMode: " + splitMode);
        }
        List<String> orgNames = samples.stream()
                .map(MappingSample::getOrgName)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        Map<String, Boolean> holdoutFlags = new HashMap<>();
        for (int index = 0; index < orgNames.size(); index++) {
            holdoutFlags.put(orgNames.get(index), index % 5 == 0);
        }
        List<MappingSample> train = samples.stream()
                .filter(sample -> !Boolean.TRUE.equals(holdoutFlags.get(sample.getOrgName())))
                .toList();
        List<MappingSample> test = samples.stream()
                .filter(sample -> Boolean.TRUE.equals(holdoutFlags.get(sample.getOrgName())))
                .toList();
        validateSplit(train, test);
        return new SplitResult(train, test);
    }

    /**
     * 从测试集中构建较小的调整子集。
     */
    public static List<MappingSample> buildTuningSubset(List<MappingSample> samples, int maxPerOrg, int maxTotal) {
        if (samples == null || samples.isEmpty()) {
            return List.of();
        }
        List<MappingSample> ordered = samples.stream()
                .sorted(Comparator
                        .comparing((MappingSample sample) -> safe(sample.getOrgName()))
                        .thenComparing(sample -> safe(sample.getExternalCode()))
                        .thenComparing(sample -> safe(sample.getExternalName())))
                .toList();
        List<MappingSample> selected = new ArrayList<>();
        Map<String, Integer> perOrgCounter = new LinkedHashMap<>();
        for (MappingSample sample : ordered) {
            String orgName = safe(sample.getOrgName());
            int orgCount = perOrgCounter.getOrDefault(orgName, 0);
            if (orgCount >= maxPerOrg) {
                continue;
            }
            selected.add(sample);
            perOrgCounter.put(orgName, orgCount + 1);
            if (selected.size() >= maxTotal) {
                break;
            }
        }
        return selected;
    }

    /**
     * 将映射样本转换为历史提示索引。
     */
    public static MappingHintIndex buildMappingHintIndex(List<MappingSample> samples) {
        Map<String, Map<HintKey, Integer>> nameCounter = new LinkedHashMap<>();
        Map<String, Map<HintKey, Integer>> codeCounter = new LinkedHashMap<>();
        if (samples != null) {
            for (MappingSample sample : samples) {
                String normalizedName = MatchTextSupport.normalizeMatchText(sample.getExternalName());
                HintKey hintKey = new HintKey(sample.getStandardCode(), sample.getStandardName());
                if (!normalizedName.isBlank()) {
                    increment(nameCounter, normalizedName, hintKey);
                }
                if (sample.getExternalCode() != null && !sample.getExternalCode().isBlank()) {
                    increment(codeCounter, sample.getExternalCode().trim(), hintKey);
                }
            }
        }
        Map<String, List<MappingHint>> hintsByName = buildHintMap(nameCounter, "history_name");
        Map<String, List<MappingHint>> hintsByCode = buildHintMap(codeCounter, "history_code");
        List<MappingHint> hints = new ArrayList<>();
        hintsByName.values().forEach(hints::addAll);
        hintsByCode.values().forEach(hints::addAll);
        return MappingHintIndex.builder()
                .hints(hints)
                .hintsByName(hintsByName)
                .hintsByCode(hintsByCode)
                .build();
    }

    /**
     * 在调整集上搜索最佳权重配置。
     */
    public static SearchResult searchMatchWeights(
            List<MappingSample> trainSamples,
            List<MappingSample> testSamples,
            List<StandardSubject> standardSubjects,
            int topK,
            MappingHintIndex mappingHintIndex,
            ValsetMatcher subjectMatcher,
            MatchRuntimeConfig runtimeConfig
    ) {
        List<MatchWeights> candidates = generateWeightCandidates();
        MatchWeights bestWeights = defaultWeights();
        Map<String, Object> bestMetrics = null;
        int evaluated = 0;
        for (MatchWeights weights : candidates) {
            Map<String, Object> metrics = runMappingEvaluation(
                    trainSamples,
                    testSamples,
                    standardSubjects,
                    weights,
                    topK,
                    mappingHintIndex,
                    subjectMatcher,
                    runtimeConfig
            );
            evaluated++;
            if (bestMetrics == null || isBetter(metrics, bestMetrics)) {
                bestMetrics = metrics;
                bestWeights = weights;
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("evaluated_weight_count", evaluated);
        report.put("selection_rule", "优先 top1_accuracy，再看 top3_recall，再看 high_confidence_top1_accuracy，再看 low_confidence_ratio 更低");
        report.put("best_metrics", bestMetrics);
        return new SearchResult(bestWeights, report);
    }

    /**
     * 评估测试集上的权重配置。
     */
    public static Map<String, Object> runMappingEvaluation(
            List<MappingSample> trainSamples,
            List<MappingSample> testSamples,
            List<StandardSubject> standardSubjects,
            MatchWeights weights,
            int topK,
            MappingHintIndex mappingHintIndex,
            ValsetMatcher subjectMatcher,
            MatchRuntimeConfig runtimeConfig
    ) {
        if (testSamples == null || testSamples.isEmpty()) {
            throw new IllegalArgumentException("Test samples are empty");
        }
        MappingHintIndex resolvedHintIndex = mappingHintIndex == null ? buildMappingHintIndex(trainSamples) : mappingHintIndex;
        MatchContext context = MatchContext.builder()
                .standardSubjects(standardSubjects)
                .mappingHintIndex(resolvedHintIndex)
                .weights(weights)
                .runtimeConfig(runtimeConfig)
                .build();

        int top1Hit = 0;
        int top3Hit = 0;
        int top5Hit = 0;
        int highConfidenceTotal = 0;
        int highConfidenceHit = 0;
        int lowConfidenceTotal = 0;
        double scoreSum = 0D;
        List<Map<String, Object>> failures = new ArrayList<>();

        for (MappingSample sample : testSamples) {
            ValsetMatchResult result = subjectMatcher.matchSubject(buildSampleSubject(sample), List.of(), context, topK);
            List<String> predictedCodes = result.getTopCandidates() == null
                    ? List.of()
                    : result.getTopCandidates().stream().map(MatchCandidate::getStandardCode).toList();
            boolean hit1 = Objects.equals(result.getMatchedStandardCode(), sample.getStandardCode());
            boolean hit3 = predictedCodes.stream().limit(3).anyMatch(sample.getStandardCode()::equals);
            boolean hit5 = predictedCodes.stream().limit(5).anyMatch(sample.getStandardCode()::equals);
            top1Hit += hit1 ? 1 : 0;
            top3Hit += hit3 ? 1 : 0;
            top5Hit += hit5 ? 1 : 0;
            scoreSum += doubleValue(result.getScore());

            String confidence = result.getConfidenceLevel() == null
                    ? "low"
                    : result.getConfidenceLevel().name().toLowerCase(Locale.ROOT);
            if ("high".equals(confidence)) {
                highConfidenceTotal++;
                highConfidenceHit += hit1 ? 1 : 0;
            }
            if ("low".equals(confidence)) {
                lowConfidenceTotal++;
            }

            if (!hit1 && failures.size() < FAILURE_SAMPLE_LIMIT) {
                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("org_name", sample.getOrgName());
                failure.put("external_code", sample.getExternalCode());
                failure.put("external_name", sample.getExternalName());
                failure.put("expected_standard_code", sample.getStandardCode());
                failure.put("expected_standard_name", sample.getStandardName());
                failure.put("predicted_standard_code", result.getMatchedStandardCode());
                failure.put("predicted_standard_name", result.getMatchedStandardName());
                failure.put("confidence", confidence);
                failure.put("score", round6(doubleValue(result.getScore())));
                failures.add(failure);
            }
        }

        int total = testSamples.size();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("sample_count", total);
        metrics.put("top1_accuracy", round6((double) top1Hit / total));
        metrics.put("top3_recall", round6((double) top3Hit / total));
        metrics.put("top5_recall", round6((double) top5Hit / total));
        metrics.put("high_confidence_count", highConfidenceTotal);
        metrics.put("high_confidence_top1_accuracy", highConfidenceTotal == 0 ? null : round6((double) highConfidenceHit / highConfidenceTotal));
        metrics.put("low_confidence_ratio", round6((double) lowConfidenceTotal / total));
        metrics.put("average_score", round6(scoreSum / total));
        metrics.put("failure_samples", failures);
        return metrics;
    }

    /**
     * 从采样的缺失中构建紧凑的故障分析。
     */
    public static Map<String, Object> buildFailureAnalysis(List<Map<String, Object>> failureSamples) {
        Map<String, Integer> expectedRootCounter = new LinkedHashMap<>();
        Map<String, Integer> predictedRootCounter = new LinkedHashMap<>();
        Map<String, Integer> rootConfusionCounter = new LinkedHashMap<>();
        Map<String, Integer> confidenceCounter = new LinkedHashMap<>();
        Map<String, Integer> orgCounter = new LinkedHashMap<>();
        Map<String, Integer> keywordCounter = new LinkedHashMap<>();

        if (failureSamples != null) {
            for (Map<String, Object> sample : failureSamples) {
                String expectedRoot = extractRoot(stringValue(sample.get("expected_standard_code")));
                String predictedRoot = extractRoot(stringValue(sample.get("predicted_standard_code")));
                increment(expectedRootCounter, expectedRoot);
                increment(predictedRootCounter, predictedRoot);
                increment(rootConfusionCounter, expectedRoot + "->" + predictedRoot);
                increment(confidenceCounter, stringValue(sample.get("confidence")));
                increment(orgCounter, stringValue(sample.get("org_name")));
                for (String keyword : MatchTextSupport.keywordSet(stringValue(sample.get("external_name")))) {
                    increment(keywordCounter, keyword);
                }
            }
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("top_expected_roots", topEntries(expectedRootCounter, 10));
        analysis.put("top_predicted_roots", topEntries(predictedRootCounter, 10));
        analysis.put("top_root_confusions", topEntries(rootConfusionCounter, 10));
        analysis.put("confidence_distribution", confidenceCounter);
        analysis.put("top_orgs", topEntries(orgCounter, 10));
        analysis.put("top_external_keywords", topEntries(keywordCounter, 15));
        return analysis;
    }

    /**
     * 返回默认的基于规则的权重配置。
     */
    public static MatchWeights defaultWeights() {
        return MatchWeights.builder()
                .nameWeight(BigDecimal.valueOf(0.22D))
                .pathWeight(BigDecimal.valueOf(0.22D))
                .keywordWeight(BigDecimal.valueOf(0.22D))
                .codeWeight(BigDecimal.valueOf(0.04D))
                .historyWeight(BigDecimal.valueOf(0.30D))
                .embeddingWeight(BigDecimal.ZERO)
                .build();
    }

    private static void validateSplit(List<MappingSample> train, List<MappingSample> test) {
        if (train == null || train.isEmpty() || test == null || test.isEmpty()) {
            throw new IllegalArgumentException("Train or test samples are empty");
        }
    }

    private static void increment(Map<String, Integer> counter, String key) {
        counter.merge(safe(key), 1, Integer::sum);
    }

    private static void increment(Map<String, Map<HintKey, Integer>> counter, String key, HintKey hintKey) {
        counter.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).merge(hintKey, 1, Integer::sum);
    }

    private static Map<String, List<MappingHint>> buildHintMap(
            Map<String, Map<HintKey, Integer>> counterMap,
            String source
    ) {
        Map<String, List<MappingHint>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<HintKey, Integer>> entry : counterMap.entrySet()) {
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            List<MappingHint> hints = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<HintKey, Integer>comparingByValue(Comparator.reverseOrder()))
                    .map(item -> MappingHint.builder()
                            .source(source)
                            .normalizedKey(entry.getKey())
                            .standardCode(item.getKey().standardCode())
                            .standardName(item.getKey().standardName())
                            .supportCount(item.getValue())
                            .confidence(BigDecimal.valueOf((double) item.getValue() / total))
                            .build())
                    .toList();
            result.put(entry.getKey(), hints);
        }
        return result;
    }

    private static List<MatchWeights> generateWeightCandidates() {
        return List.of(
                MatchWeights.builder().nameWeight(BigDecimal.valueOf(0.22D)).pathWeight(BigDecimal.valueOf(0.22D)).keywordWeight(BigDecimal.valueOf(0.22D)).codeWeight(BigDecimal.valueOf(0.04D)).historyWeight(BigDecimal.valueOf(0.30D)).embeddingWeight(BigDecimal.ZERO).build(),
                MatchWeights.builder().nameWeight(BigDecimal.valueOf(0.18D)).pathWeight(BigDecimal.valueOf(0.18D)).keywordWeight(BigDecimal.valueOf(0.29D)).codeWeight(BigDecimal.valueOf(0.05D)).historyWeight(BigDecimal.valueOf(0.30D)).embeddingWeight(BigDecimal.ZERO).build(),
                MatchWeights.builder().nameWeight(BigDecimal.valueOf(0.24D)).pathWeight(BigDecimal.valueOf(0.24D)).keywordWeight(BigDecimal.valueOf(0.18D)).codeWeight(BigDecimal.valueOf(0.04D)).historyWeight(BigDecimal.valueOf(0.30D)).embeddingWeight(BigDecimal.ZERO).build(),
                MatchWeights.builder().nameWeight(BigDecimal.valueOf(0.20D)).pathWeight(BigDecimal.valueOf(0.20D)).keywordWeight(BigDecimal.valueOf(0.26D)).codeWeight(BigDecimal.valueOf(0.04D)).historyWeight(BigDecimal.valueOf(0.30D)).embeddingWeight(BigDecimal.ZERO).build(),
                MatchWeights.builder().nameWeight(BigDecimal.valueOf(0.25D)).pathWeight(BigDecimal.valueOf(0.20D)).keywordWeight(BigDecimal.valueOf(0.20D)).codeWeight(BigDecimal.valueOf(0.05D)).historyWeight(BigDecimal.valueOf(0.30D)).embeddingWeight(BigDecimal.ZERO).build(),
                MatchWeights.builder().nameWeight(BigDecimal.valueOf(0.18D)).pathWeight(BigDecimal.valueOf(0.22D)).keywordWeight(BigDecimal.valueOf(0.25D)).codeWeight(BigDecimal.valueOf(0.05D)).historyWeight(BigDecimal.valueOf(0.30D)).embeddingWeight(BigDecimal.ZERO).build()
        );
    }

    private static boolean isBetter(Map<String, Object> current, Map<String, Object> best) {
        List<Double> currentKey = List.of(
                metricValue(current, "top1_accuracy"),
                metricValue(current, "top3_recall"),
                metricValue(current, "high_confidence_top1_accuracy"),
                -metricValue(current, "low_confidence_ratio")
        );
        List<Double> bestKey = List.of(
                metricValue(best, "top1_accuracy"),
                metricValue(best, "top3_recall"),
                metricValue(best, "high_confidence_top1_accuracy"),
                -metricValue(best, "low_confidence_ratio")
        );
        for (int index = 0; index < currentKey.size(); index++) {
            int compare = Double.compare(currentKey.get(index), bestKey.get(index));
            if (compare != 0) {
                return compare > 0;
            }
        }
        return false;
    }

    private static SubjectRecord buildSampleSubject(MappingSample sample) {
        String externalCode = safe(sample.getExternalCode());
        int level = externalCode.isBlank() ? 1 : Math.max(1, externalCode.split("\\.").length);
        String rootCode = externalCode.isBlank() ? "" : externalCode.split("\\.")[0];
        List<String> pathCodes = externalCode.isBlank() ? List.of() : List.of(externalCode);
        return SubjectRecord.builder()
                .sheetName("mapping_eval")
                .rowDataNumber(0)
                .subjectCode(externalCode)
                .subjectName(sample.getExternalName())
                .level(level)
                .parentCode(null)
                .rootCode(rootCode)
                .segmentCount(level)
                .pathCodes(pathCodes)
                .leaf(Boolean.TRUE)
                .build();
    }

    private static int stableBucket(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            String text = java.util.Arrays.stream(parts).map(MappingEvaluationSupport::safe).collect(Collectors.joining("|"));
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            long value = ((bytes[0] & 0xffL) << 24)
                    | ((bytes[1] & 0xffL) << 16)
                    | ((bytes[2] & 0xffL) << 8)
                    | (bytes[3] & 0xffL);
            return (int) (value % 5);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 algorithm unavailable", exception);
        }
    }

    private static String extractRoot(String subjectCode) {
        String code = safe(subjectCode);
        if (code.isBlank()) {
            return "";
        }
        return code.contains(".") ? code.substring(0, code.indexOf('.')) : code;
    }

    private static List<List<Object>> topEntries(Map<String, Integer> counter, int limit) {
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> {
                    List<Object> row = new ArrayList<>(2);
                    row.add(entry.getKey());
                    row.add(entry.getValue());
                    return row;
                })
                .toList();
    }

    private static double metricValue(Map<String, Object> metrics, String key) {
        Object value = metrics.get(key);
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static double round6(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private static double doubleValue(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record HintKey(String standardCode, String standardName) {
    }

    public record SplitResult(List<MappingSample> trainSamples, List<MappingSample> testSamples) {
    }

    public record SearchResult(MatchWeights bestWeights, Map<String, Object> report) {
    }
}
