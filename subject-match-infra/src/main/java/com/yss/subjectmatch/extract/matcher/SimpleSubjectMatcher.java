package com.yss.subjectmatch.extract.matcher;

import com.yss.subjectmatch.domain.matcher.EmbeddingProvider;
import com.yss.subjectmatch.domain.matcher.SubjectMatcher;
import com.yss.subjectmatch.domain.model.*;
import com.yss.subjectmatch.extract.support.MatchTextSupport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Java 迁移使用的基于规则的科目匹配器。
 */
@Component
public class SimpleSubjectMatcher implements SubjectMatcher {

    private final EmbeddingProvider embeddingProvider;

    public SimpleSubjectMatcher(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    /**
     * 一次性匹配所有科目。
     */
    @Override
    public List<SubjectMatchResult> matchSubjects(List<SubjectRecord> subjects, MatchContext context, int topK) {
        MatchingState matchingState = prepareState(context);
        List<SubjectMatchResult> results = new ArrayList<>(subjects.size());
        for (SubjectRecord subject : subjects) {
            results.add(matchSubjectInternal(subject, subjects, context, matchingState, topK));
        }
        return results;
    }

    /**
     * 将单个科目与完整科目集进行匹配。
     */
    @Override
    public SubjectMatchResult matchSubject(SubjectRecord subject, List<SubjectRecord> allSubjects, MatchContext context,
            int topK) {
        return matchSubjectInternal(subject, allSubjects, context, prepareState(context), topK);
    }

    private SubjectMatchResult matchSubjectInternal(
            SubjectRecord subject,
            List<SubjectRecord> allSubjects,
            MatchContext context,
            MatchingState state,
            int topK) {
        if (state.standardSubjects().isEmpty()) {
            return emptyResult(subject);
        }

        // 核心步骤 1：寻找匹配基准节点 (Anchor Subject)
        AnchorSelection anchorSelection = selectAnchor(subject, allSubjects);

        // 核心步骤 2：对标准科目进行大范围召回 (Recall candidates)
        List<CandidateSource> candidatePool = recallCandidates(anchorSelection.anchorSubject(),
                anchorSelection.anchorPathText(), context, state);
        boolean initialUseEmbedding = !"low_confidence".equalsIgnoreCase(runtimeConfig(context).getEmbeddingStrategy());

        // 核心步骤 3：多维度计算相似分数并进行打分排序 (Score candidates)
        List<MatchCandidate> scoredCandidates = scoreCandidates(subject, anchorSelection, context, state, candidatePool,
                initialUseEmbedding);
        if (scoredCandidates.isEmpty()) {
            return emptyResult(subject);
        }

        // 核心步骤 4：截取Top K候选并评定置信度级别 (Classify confidence)
        List<MatchCandidate> topCandidates = limitCandidates(scoredCandidates, topK);
        MatchCandidate best = topCandidates.get(0);
        MatchCandidate second = topCandidates.size() > 1 ? topCandidates.get(1) : null;
        ConfidenceLevel confidenceLevel = classifyConfidence(best, second);

        // 核心步骤 5：处理层级规则及特定业务重写规则 (Hierarchy and rule override)
        OverrideResult overrideResult = hierarchyOverrideCandidate(subject, allSubjects, topCandidates, state);
        if (overrideResult.candidate() != null) {
            topCandidates = mergeOverrideCandidate(overrideResult.candidate(), topCandidates, topK);
            best = topCandidates.get(0);
            second = topCandidates.size() > 1 ? topCandidates.get(1) : null;
            confidenceLevel = classifyConfidence(best, second);
        }

        // 核心步骤 6：如果为低置信度且允许兜底，则开启向量检索再召回一次 (Fallback embedding strategy)
        if (shouldEnableEmbedding(context)
                && "low_confidence".equalsIgnoreCase(runtimeConfig(context).getEmbeddingStrategy())
                && confidenceLevel == ConfidenceLevel.LOW) {
            scoredCandidates = scoreCandidates(subject, anchorSelection, context, state, candidatePool, true);
            topCandidates = limitCandidates(scoredCandidates, topK);
            best = topCandidates.get(0);
            second = topCandidates.size() > 1 ? topCandidates.get(1) : null;
            confidenceLevel = classifyConfidence(best, second);
        }

        String matchReason = String.join("; ", best.getReasons());
        if (!overrideResult.reason().isBlank()) {
            matchReason = matchReason.isBlank() ? overrideResult.reason()
                    : matchReason + "; " + overrideResult.reason();
        }

        return SubjectMatchResult.builder()
                .externalSubjectCode(subject.getSubjectCode())
                .externalSubjectName(subject.getSubjectName())
                .externalLevel(subject.getLevel())
                .externalIsLeaf(Boolean.TRUE.equals(subject.getLeaf()))
                .anchorSubjectCode(anchorSelection.anchorSubject().getSubjectCode())
                .anchorSubjectName(anchorSelection.anchorSubject().getSubjectName())
                .anchorLevel(anchorSelection.anchorSubject().getLevel())
                .anchorPathText(anchorSelection.anchorPathText())
                .anchorReason(anchorSelection.reason())
                .matchedStandardCode(best.getStandardCode())
                .matchedStandardName(best.getStandardName())
                .score(best.getScore())
                .scoreName(best.getScoreName())
                .scorePath(best.getScorePath())
                .scoreKeyword(best.getScoreKeyword())
                .scoreCode(best.getScoreCode())
                .scoreHistory(best.getScoreHistory())
                .scoreEmbedding(best.getScoreEmbedding())
                .confidenceLevel(confidenceLevel)
                .needsReview(confidenceLevel != ConfidenceLevel.HIGH)
                .matchReason(matchReason)
                .candidateCount(topCandidates.size())
                .topCandidates(topCandidates)
                .build();
    }

    private SubjectMatchResult emptyResult(SubjectRecord subject) {
        return SubjectMatchResult.builder()
                .externalSubjectCode(subject.getSubjectCode())
                .externalSubjectName(subject.getSubjectName())
                .externalLevel(subject.getLevel())
                .externalIsLeaf(Boolean.TRUE.equals(subject.getLeaf()))
                .anchorSubjectCode(subject.getSubjectCode())
                .anchorSubjectName(subject.getSubjectName())
                .anchorLevel(subject.getLevel())
                .anchorPathText(subject.getSubjectName())
                .anchorReason("fallback_self")
                .matchedStandardCode("")
                .matchedStandardName("")
                .score(BigDecimal.ZERO)
                .scoreName(BigDecimal.ZERO)
                .scorePath(BigDecimal.ZERO)
                .scoreKeyword(BigDecimal.ZERO)
                .scoreCode(BigDecimal.ZERO)
                .scoreHistory(BigDecimal.ZERO)
                .scoreEmbedding(BigDecimal.ZERO)
                .confidenceLevel(ConfidenceLevel.LOW)
                .needsReview(Boolean.TRUE)
                .matchReason("No standard subject candidate available")
                .candidateCount(0)
                .topCandidates(List.of())
                .build();
    }

    private MatchingState prepareState(MatchContext context) {
        List<StandardSubject> standardSubjects = context == null || context.getStandardSubjects() == null
                ? List.of()
                : context.getStandardSubjects();
        Map<String, StandardSubject> standardByCode = new LinkedHashMap<>();
        Map<String, List<StandardSubject>> standardByRoot = new LinkedHashMap<>();
        Map<String, Set<String>> standardCodesByKeyword = new LinkedHashMap<>();
        Map<String, Set<String>> standardCodesByNameToken = new LinkedHashMap<>();
        Map<String, Set<String>> standardCodesByPathToken = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> nameCounters = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> pathCounters = new LinkedHashMap<>();
        Map<String, Set<String>> keywordSets = new LinkedHashMap<>();
        Map<String, float[]> standardEmbeddings = new LinkedHashMap<>();

        for (StandardSubject standardSubject : standardSubjects) {
            standardByCode.put(standardSubject.getStandardCode(), standardSubject);
            standardByRoot.computeIfAbsent(standardSubject.getRootCode(), ignored -> new ArrayList<>())
                    .add(standardSubject);

            Map<String, Integer> nameCounter = MatchTextSupport.tokenCounter(standardSubject.getStandardName());
            Map<String, Integer> pathCounter = MatchTextSupport.tokenCounter(standardSubject.getPathText());
            Set<String> keywords = MatchTextSupport.keywordSet(standardSubject.getPathText());
            nameCounters.put(standardSubject.getStandardCode(), nameCounter);
            pathCounters.put(standardSubject.getStandardCode(), pathCounter);
            keywordSets.put(standardSubject.getStandardCode(), keywords);

            for (String keyword : keywords) {
                standardCodesByKeyword.computeIfAbsent(keyword, ignored -> new HashSet<>())
                        .add(standardSubject.getStandardCode());
            }
            for (String token : nameCounter.keySet()) {
                standardCodesByNameToken.computeIfAbsent(token, ignored -> new HashSet<>())
                        .add(standardSubject.getStandardCode());
            }
            for (String token : pathCounter.keySet()) {
                standardCodesByPathToken.computeIfAbsent(token, ignored -> new HashSet<>())
                        .add(standardSubject.getStandardCode());
            }
        }

        if (shouldEnableEmbedding(context)) {
            List<String> texts = standardSubjects.stream()
                    .map(StandardSubject::getPathText)
                    .toList();
            List<float[]> embeddings = embeddingProvider.encodeDocuments(texts, runtimeConfig(context));
            for (int index = 0; index < Math.min(standardSubjects.size(), embeddings.size()); index++) {
                standardEmbeddings.put(standardSubjects.get(index).getStandardCode(), embeddings.get(index));
            }
        }

        return new MatchingState(
                standardSubjects,
                standardByCode,
                standardByRoot,
                standardCodesByKeyword,
                standardCodesByNameToken,
                standardCodesByPathToken,
                nameCounters,
                pathCounters,
                keywordSets,
                standardEmbeddings);
    }

    private AnchorSelection selectAnchor(SubjectRecord subject, List<SubjectRecord> subjects) {
        Map<String, SubjectRecord> subjectMap = new LinkedHashMap<>();
        for (SubjectRecord item : subjects) {
            subjectMap.put(item.getSubjectCode(), item);
        }
        List<SubjectRecord> lineage = buildLineage(subject, subjectMap);
        if (lineage.isEmpty()) {
            return new AnchorSelection(subject, List.of(subject.getSubjectName()), subject.getSubjectName(),
                    "fallback_self");
        }
        for (int index = lineage.size() - 1; index >= 0; index--) {
            SubjectRecord candidate = lineage.get(index);
            if (MatchTextSupport.isBusinessLikeName(candidate.getSubjectName()) && !shouldSkipAnchor(candidate)) {
                List<String> pathNames = lineage.subList(0, index + 1).stream().map(SubjectRecord::getSubjectName)
                        .toList();
                return new AnchorSelection(candidate, pathNames, MatchTextSupport.buildPathText(pathNames),
                        "first_business_node_from_leaf");
            }
        }
        for (int index = lineage.size() - 2; index >= 0; index--) {
            SubjectRecord candidate = lineage.get(index);
            if (!MatchTextSupport.isInstanceLikeName(candidate.getSubjectName())) {
                List<String> pathNames = lineage.subList(0, index + 1).stream().map(SubjectRecord::getSubjectName)
                        .toList();
                return new AnchorSelection(candidate, pathNames, MatchTextSupport.buildPathText(pathNames),
                        "fallback_non_instance_parent");
            }
        }
        SubjectRecord root = lineage.get(0);
        return new AnchorSelection(root, List.of(root.getSubjectName()), root.getSubjectName(), "fallback_root");
    }

    private List<SubjectRecord> buildLineage(SubjectRecord subject, Map<String, SubjectRecord> subjectMap) {
        List<SubjectRecord> lineage = new ArrayList<>();
        SubjectRecord current = subject;
        while (current != null) {
            lineage.add(0, current);
            current = current.getParentCode() == null ? null : subjectMap.get(current.getParentCode());
        }
        return lineage;
    }

    private boolean shouldSkipAnchor(SubjectRecord candidate) {
        if (MatchTextSupport.isInstanceLikeName(candidate.getSubjectName())) {
            return true;
        }
        return Set.of("上交所", "深交所", "银行间").contains(candidate.getSubjectName());
    }

    private List<CandidateSource> recallCandidates(
            SubjectRecord anchorSubject,
            String anchorPathText,
            MatchContext context,
            MatchingState state) {
        Map<String, Integer> anchorNameCounter = MatchTextSupport.tokenCounter(anchorSubject.getSubjectName());
        Map<String, Integer> anchorPathCounter = MatchTextSupport.tokenCounter(anchorPathText);
        Set<String> anchorKeywords = MatchTextSupport.keywordSet(anchorPathText);
        List<StandardSubject> scanPool = buildScanPool(anchorSubject, context, state, anchorKeywords, anchorNameCounter,
                anchorPathCounter);

        Map<String, Set<String>> candidateSources = new LinkedHashMap<>();
        for (StandardSubject standard : scanPool) {
            double nameScore = MatchTextSupport.cosineSimilarity(anchorNameCounter,
                    state.nameCounters().get(standard.getStandardCode()));
            double pathScore = MatchTextSupport.cosineSimilarity(anchorPathCounter,
                    state.pathCounters().get(standard.getStandardCode()));
            double codeScore = codeSimilarity(anchorSubject.getSubjectCode(), standard.getStandardCode());
            boolean keywordOverlap = !MatchTextSupport
                    .intersection(anchorKeywords, state.keywordSets().get(standard.getStandardCode())).isEmpty();
            Set<String> sources = new HashSet<>();
            if (nameScore >= 0.45D) {
                sources.add("name");
            }
            if (pathScore >= 0.40D) {
                sources.add("path");
            }
            if (codeScore >= 0.60D) {
                sources.add("code");
            }
            if (keywordOverlap) {
                sources.add("keyword");
            }
            if (!sources.isEmpty()) {
                candidateSources.put(standard.getStandardCode(), sources);
            }
        }

        for (String standardCode : historyBoostCodes(context.getMappingHintIndex(), anchorSubject.getSubjectCode(),
                anchorSubject.getSubjectName())) {
            candidateSources.computeIfAbsent(standardCode, ignored -> new HashSet<>()).add("history");
        }

        if (shouldEnableEmbedding(context)) {
            for (String standardCode : embeddingCandidateCodes(anchorPathText, state, runtimeConfig(context))) {
                if (candidateSources.containsKey(standardCode)) {
                    candidateSources.get(standardCode).add("embedding");
                }
            }
        }

        if (candidateSources.isEmpty()) {
            List<StandardSubject> fallbackPool = state.standardByRoot()
                    .getOrDefault(extractRootKey(anchorSubject.getSubjectCode()), state.standardSubjects());
            for (StandardSubject standardSubject : fallbackPool) {
                candidateSources.put(standardSubject.getStandardCode(), new HashSet<>(Set.of("fallback")));
            }
        }

        List<CandidateSource> candidates = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : candidateSources.entrySet()) {
            StandardSubject standardSubject = state.standardByCode().get(entry.getKey());
            if (standardSubject != null) {
                List<String> orderedSources = new ArrayList<>(entry.getValue());
                orderedSources.sort(String::compareTo);
                candidates.add(new CandidateSource(standardSubject, orderedSources));
            }
        }
        return candidates;
    }

    private List<StandardSubject> buildScanPool(
            SubjectRecord anchorSubject,
            MatchContext context,
            MatchingState state,
            Set<String> anchorKeywords,
            Map<String, Integer> anchorNameCounter,
            Map<String, Integer> anchorPathCounter) {
        String rootKey = extractRootKey(anchorSubject.getSubjectCode());
        Map<String, Integer> sourcePriority = new LinkedHashMap<>();
        Set<String> candidateCodes = new HashSet<>();

        for (StandardSubject standardSubject : state.standardByRoot().getOrDefault(rootKey, List.of())) {
            candidateCodes.add(standardSubject.getStandardCode());
            sourcePriority.merge(standardSubject.getStandardCode(), 4, Math::max);
        }
        for (String keyword : anchorKeywords) {
            for (String standardCode : state.standardCodesByKeyword().getOrDefault(keyword, Set.of())) {
                candidateCodes.add(standardCode);
                sourcePriority.merge(standardCode, 3, Math::max);
            }
        }
        for (String token : anchorNameCounter.keySet()) {
            for (String standardCode : state.standardCodesByNameToken().getOrDefault(token, Set.of())) {
                candidateCodes.add(standardCode);
                sourcePriority.merge(standardCode, 2, Math::max);
            }
        }
        for (String token : anchorPathCounter.keySet()) {
            for (String standardCode : state.standardCodesByPathToken().getOrDefault(token, Set.of())) {
                candidateCodes.add(standardCode);
                sourcePriority.merge(standardCode, 1, Math::max);
            }
        }
        for (String standardCode : historyBoostCodes(context.getMappingHintIndex(), anchorSubject.getSubjectCode(),
                anchorSubject.getSubjectName())) {
            candidateCodes.add(standardCode);
            sourcePriority.merge(standardCode, 5, Math::max);
        }

        if (candidateCodes.isEmpty()) {
            return state.standardSubjects();
        }

        List<String> rankedCodes = new ArrayList<>(candidateCodes);
        rankedCodes.sort((left, right) -> {
            int leftPriority = sourcePriority.getOrDefault(left, 0);
            int rightPriority = sourcePriority.getOrDefault(right, 0);
            if (leftPriority != rightPriority) {
                return Integer.compare(rightPriority, leftPriority);
            }
            int leftOverlap = MatchTextSupport
                    .intersection(anchorKeywords, state.keywordSets().getOrDefault(left, Set.of())).size();
            int rightOverlap = MatchTextSupport
                    .intersection(anchorKeywords, state.keywordSets().getOrDefault(right, Set.of())).size();
            if (leftOverlap != rightOverlap) {
                return Integer.compare(rightOverlap, leftOverlap);
            }
            double leftScore = MatchTextSupport.cosineSimilarity(anchorNameCounter,
                    state.nameCounters().getOrDefault(left, Map.of()));
            double rightScore = MatchTextSupport.cosineSimilarity(anchorNameCounter,
                    state.nameCounters().getOrDefault(right, Map.of()));
            if (Double.compare(leftScore, rightScore) != 0) {
                return Double.compare(rightScore, leftScore);
            }
            return right.compareTo(left);
        });
        if (rankedCodes.size() > 600) {
            rankedCodes = rankedCodes.subList(0, 600);
        }
        List<StandardSubject> pool = new ArrayList<>(rankedCodes.size());
        for (String rankedCode : rankedCodes) {
            pool.add(state.standardByCode().get(rankedCode));
        }
        return pool;
    }

    private List<MatchCandidate> scoreCandidates(
            SubjectRecord subject,
            AnchorSelection anchorSelection,
            MatchContext context,
            MatchingState state,
            List<CandidateSource> candidatePool,
            boolean useEmbedding) {
        MatchWeights weights = effectiveWeights(context == null ? null : context.getWeights());
        Map<String, Integer> anchorNameCounter = MatchTextSupport
                .tokenCounter(anchorSelection.anchorSubject().getSubjectName());
        Map<String, Integer> anchorPathCounter = MatchTextSupport.tokenCounter(anchorSelection.anchorPathText());
        float[] anchorEmbedding = useEmbedding
                ? embeddingProvider.encodeQuery(anchorSelection.anchorPathText(), runtimeConfig(context))
                : new float[0];
        List<MatchCandidate> scored = new ArrayList<>(candidatePool.size());

        for (CandidateSource candidateSource : candidatePool) {
            StandardSubject standard = candidateSource.standardSubject();
            double scoreName = nameSimilarity(anchorSelection.anchorSubject().getSubjectName(),
                    standard.getStandardName(), anchorNameCounter,
                    state.nameCounters().get(standard.getStandardCode()));
            double scorePath = pathSimilarity(anchorSelection.anchorPathText(), standard.getPathText(),
                    anchorPathCounter, state.pathCounters().get(standard.getStandardCode()));
            double scoreKeyword = keywordSimilarity(anchorSelection.anchorPathText(), standard.getPathText());
            double scoreCode = codeSimilarity(anchorSelection.anchorSubject().getSubjectCode(),
                    standard.getStandardCode());
            HistoryScore historyScore = historyScore(context == null ? null : context.getMappingHintIndex(), subject,
                    anchorSelection.anchorSubject(), standard);
            int missingCriticalSignals = missingCriticalSignalCount(anchorSelection.anchorPathText(),
                    standard.getPathText());
            int extraCriticalSignals = extraCriticalSignalCount(anchorSelection.anchorPathText(),
                    standard.getPathText());
            double scoreEmbedding = effectiveEmbeddingScore(
                    anchorSelection.anchorPathText(),
                    standard,
                    candidateSource.sources(),
                    scorePath,
                    scoreKeyword,
                    missingCriticalSignals,
                    extraCriticalSignals,
                    anchorEmbedding,
                    state);
            double ruleAdjustment = domainRuleAdjustment(subject.getSubjectCode(), anchorSelection.anchorPathText(),
                    standard.getStandardCode(), standard.getPathText());
            double finalScore = combineScores(weights, scoreName, scorePath, scoreKeyword, scoreCode,
                    historyScore.score(), scoreEmbedding);
            if (missingCriticalSignals > 0) {
                finalScore -= 0.08D * missingCriticalSignals;
            }
            if (extraCriticalSignals > 0) {
                finalScore -= 0.06D * extraCriticalSignals;
            }
            finalScore += ruleAdjustment;
            if (historyScore.score() >= 0.95D && missingCriticalSignals == 0) {
                finalScore = Math.max(finalScore, 0.93D);
            }
            List<String> reasons = new ArrayList<>();
            reasons.add("name=" + formatScore(scoreName));
            reasons.add("path=" + formatScore(scorePath));
            reasons.add("keyword=" + formatScore(scoreKeyword));
            reasons.add("code=" + formatScore(scoreCode));
            if (useEmbedding && toDouble(weights.getEmbeddingWeight()) > 0D) {
                reasons.add("embedding=" + formatScore(scoreEmbedding));
            }
            if (missingCriticalSignals > 0) {
                reasons.add("missing_critical=" + missingCriticalSignals);
            }
            if (extraCriticalSignals > 0) {
                reasons.add("extra_critical=" + extraCriticalSignals);
            }
            if (Math.abs(ruleAdjustment) > 0.000001D) {
                reasons.add("rule_adjustment=" + formatScore(ruleAdjustment));
            }
            if (!historyScore.reason().isBlank()) {
                reasons.add(historyScore.reason());
            }
            if (Boolean.TRUE.equals(standard.getPlaceholder())) {
                finalScore *= 0.92D;
                reasons.add("placeholder_penalty");
            }
            scored.add(MatchCandidate.builder()
                    .standardCode(standard.getStandardCode())
                    .standardName(standard.getStandardName())
                    .score(decimal(finalScore))
                    .scoreName(decimal(scoreName))
                    .scorePath(decimal(scorePath))
                    .scoreKeyword(decimal(scoreKeyword))
                    .scoreCode(decimal(scoreCode))
                    .scoreHistory(decimal(historyScore.score()))
                    .scoreEmbedding(decimal(scoreEmbedding))
                    .matchedByHistory(!historyScore.reason().isBlank())
                    .candidateSources(candidateSource.sources())
                    .reasons(reasons)
                    .build());
        }

        scored.sort(
                Comparator.comparing(MatchCandidate::getStandardCode, Comparator.nullsLast(Comparator.reverseOrder())));
        scored.sort(Comparator.comparing(MatchCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        return scored;
    }

    private double effectiveEmbeddingScore(
            String anchorPathText,
            StandardSubject standard,
            List<String> candidateSources,
            double scorePath,
            double scoreKeyword,
            int missingCriticalSignals,
            int extraCriticalSignals,
            float[] anchorEmbedding,
            MatchingState state) {
        double rawScore = embeddingSimilarity(anchorEmbedding, standard.getStandardCode(), state);
        if (rawScore <= 0D) {
            return 0D;
        }
        double gate = 1D;
        Set<String> sourceSet = new HashSet<>(candidateSources);
        if (MatchTextSupport.intersection(sourceSet, Set.of("name", "path", "keyword", "history", "code")).isEmpty()) {
            gate *= 0D;
        }
        if (scorePath < 0.45D && scoreKeyword < 0.40D) {
            gate *= 0.25D;
        }
        if (scorePath < 0.35D && scoreKeyword < 0.25D) {
            gate *= 0D;
        }
        if (missingCriticalSignals > 0) {
            gate *= 0.15D;
        }
        if (extraCriticalSignals > 0) {
            gate *= 0.35D;
        }
        Set<String> anchorSignals = MatchTextSupport.domainSignalSet(anchorPathText);
        Set<String> standardSignals = new HashSet<>(
                state.keywordSets().getOrDefault(standard.getStandardCode(), Set.of()));
        standardSignals.addAll(MatchTextSupport.domainSignalSet(standard.getPathText()));
        for (List<String> conflict : List.of(
                List.of("交易手续费", "结算服务费"),
                List.of("结算服务费", "交易手续费"),
                List.of("场外", "深交所"),
                List.of("场外", "上交所"),
                List.of("深交所", "场外"),
                List.of("上交所", "场外"),
                List.of("网上", "网下"),
                List.of("网下", "网上"))) {
            if (anchorSignals.contains(conflict.get(0)) && standardSignals.contains(conflict.get(1))) {
                gate *= 0D;
                break;
            }
        }
        return rawScore * gate;
    }

    private Set<String> embeddingCandidateCodes(String anchorPathText, MatchingState state,
            MatchRuntimeConfig runtimeConfig) {
        if (!shouldEnableEmbeddingForRecall(state)) {
            return Set.of();
        }
        float[] queryEmbedding = embeddingProvider.encodeQuery(anchorPathText, runtimeConfig);
        if (queryEmbedding.length == 0) {
            return Set.of();
        }
        int limit = runtimeConfig.getEmbeddingTopK() == null ? 80 : Math.max(1, runtimeConfig.getEmbeddingTopK());
        List<Map.Entry<String, float[]>> ranked = new ArrayList<>(state.standardEmbeddings().entrySet());
        ranked.sort((left, right) -> Double.compare(
                embeddingProvider.cosineSimilarity(queryEmbedding, right.getValue()),
                embeddingProvider.cosineSimilarity(queryEmbedding, left.getValue())));
        Set<String> result = new HashSet<>();
        for (int index = 0; index < Math.min(limit, ranked.size()); index++) {
            result.add(ranked.get(index).getKey());
        }
        return result;
    }

    private double embeddingSimilarity(float[] anchorEmbedding, String standardCode, MatchingState state) {
        float[] standardEmbedding = state.standardEmbeddings().get(standardCode);
        if (anchorEmbedding == null || anchorEmbedding.length == 0 || standardEmbedding == null
                || standardEmbedding.length == 0) {
            return 0D;
        }
        return embeddingProvider.cosineSimilarity(anchorEmbedding, standardEmbedding);
    }

    private HistoryScore historyScore(
            MappingHintIndex mappingHintIndex,
            SubjectRecord subject,
            SubjectRecord anchorSubject,
            StandardSubject standardSubject) {
        if (mappingHintIndex == null) {
            return new HistoryScore(0D, "");
        }
        double score = 0D;
        List<String> reasons = new ArrayList<>();
        for (MappingHint hint : mappingHintIndex.findCodeHints(subject.getSubjectCode())) {
            if (Objects.equals(hint.getStandardCode(), standardSubject.getStandardCode())) {
                double hintScore = 0.6D + 0.4D * toDouble(hint.getConfidence());
                score = Math.max(score, Math.min(1D, hintScore));
                reasons.add("history_code=" + formatScore(toDouble(hint.getConfidence()), 2));
                break;
            }
        }
        String normalizedAnchorName = MatchTextSupport.normalizeMatchText(anchorSubject.getSubjectName());
        for (MappingHint hint : mappingHintIndex.findNameHints(normalizedAnchorName)) {
            if (Objects.equals(hint.getStandardCode(), standardSubject.getStandardCode())) {
                double hintScore = 0.45D + 0.55D * toDouble(hint.getConfidence());
                score = Math.max(score, Math.min(1D, hintScore));
                reasons.add("history_name=" + formatScore(toDouble(hint.getConfidence()), 2));
                break;
            }
        }
        return new HistoryScore(score, String.join(",", reasons));
    }

    private Set<String> historyBoostCodes(MappingHintIndex mappingHintIndex, String subjectCode, String subjectName) {
        if (mappingHintIndex == null) {
            return Set.of();
        }
        Set<String> codes = new HashSet<>();
        for (MappingHint hint : mappingHintIndex.findCodeHints(subjectCode)) {
            if (toDouble(hint.getConfidence()) >= 0.60D) {
                codes.add(hint.getStandardCode());
            }
        }
        String normalizedName = MatchTextSupport.normalizeMatchText(subjectName);
        for (MappingHint hint : mappingHintIndex.findNameHints(normalizedName)) {
            if (toDouble(hint.getConfidence()) >= 0.60D) {
                codes.add(hint.getStandardCode());
            }
        }
        return codes;
    }

    private OverrideResult hierarchyOverrideCandidate(
            SubjectRecord subject,
            List<SubjectRecord> allSubjects,
            List<MatchCandidate> topCandidates,
            MatchingState state) {
        if (Boolean.TRUE.equals(subject.getLeaf())) {
            return new OverrideResult(null, "");
        }
        List<SubjectRecord> childSubjects = allSubjects.stream()
                .filter(item -> Objects.equals(item.getParentCode(), subject.getSubjectCode()))
                .toList();
        if (childSubjects.isEmpty()) {
            return new OverrideResult(null, "");
        }
        List<String> childNames = childSubjects.stream()
                .map(item -> MatchTextSupport.normalizeMatchText(item.getSubjectName()))
                .toList();
        String subjectName = MatchTextSupport.normalizeMatchText(subject.getSubjectName());

        String overrideCode = "";
        double overrideScore = 0D;
        if ("应付银行间交易费用".equals(subjectName)) {
            overrideCode = "2209.02";
            overrideScore = 0.90D;
        } else if (Set.of("中债登手续费", "上清所手续费").contains(subjectName)) {
            boolean hasTrade = childNames.stream().anyMatch(name -> name.contains("交易手续费"));
            boolean hasSettlement = childNames.stream().anyMatch(name -> name.contains("结算服务费"));
            if (hasTrade && hasSettlement) {
                overrideCode = "2209.02";
                overrideScore = 0.88D;
            } else if (hasSettlement) {
                overrideCode = "2209.02.01";
                overrideScore = 0.88D;
            }
        }
        if (overrideCode.isBlank()) {
            return new OverrideResult(null, "");
        }
        StandardSubject standardSubject = state.standardByCode().get(overrideCode);
        if (standardSubject == null) {
            return new OverrideResult(null, "");
        }
        String finalOverrideCode = overrideCode;
        MatchCandidate existing = topCandidates.stream()
                .filter(candidate -> Objects.equals(candidate.getStandardCode(), finalOverrideCode))
                .findFirst()
                .orElse(null);
        List<String> reasons = existing == null ? new ArrayList<>(List.of("hierarchy_override"))
                : new ArrayList<>(existing.getReasons());
        reasons.add("hierarchy_children=" + childSubjects.size());
        MatchCandidate overridden = MatchCandidate.builder()
                .standardCode(standardSubject.getStandardCode())
                .standardName(standardSubject.getStandardName())
                .score(decimal(Math.max(existing == null ? 0D : toDouble(existing.getScore()), overrideScore)))
                .scoreName(existing == null ? BigDecimal.ZERO : existing.getScoreName())
                .scorePath(existing == null ? BigDecimal.ZERO : existing.getScorePath())
                .scoreKeyword(existing == null ? BigDecimal.ZERO : existing.getScoreKeyword())
                .scoreCode(existing == null ? BigDecimal.ZERO : existing.getScoreCode())
                .scoreHistory(existing == null ? BigDecimal.ZERO : existing.getScoreHistory())
                .scoreEmbedding(existing == null ? BigDecimal.ZERO : existing.getScoreEmbedding())
                .matchedByHistory(existing != null && Boolean.TRUE.equals(existing.getMatchedByHistory()))
                .candidateSources(existing == null ? List.of("hierarchy_override") : existing.getCandidateSources())
                .reasons(reasons)
                .build();
        return new OverrideResult(overridden, "hierarchy_override=" + overrideCode);
    }

    private List<MatchCandidate> mergeOverrideCandidate(MatchCandidate overrideCandidate,
            List<MatchCandidate> topCandidates, int topK) {
        List<MatchCandidate> merged = new ArrayList<>();
        merged.add(overrideCandidate);
        for (MatchCandidate topCandidate : topCandidates) {
            if (!Objects.equals(topCandidate.getStandardCode(), overrideCandidate.getStandardCode())) {
                merged.add(topCandidate);
            }
        }
        merged.sort(Comparator.comparing(MatchCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        return limitCandidates(merged, topK);
    }

    private List<MatchCandidate> limitCandidates(List<MatchCandidate> candidates, int topK) {
        int size = Math.max(1, topK);
        if (candidates.size() <= size) {
            return new ArrayList<>(candidates);
        }
        return new ArrayList<>(candidates.subList(0, size));
    }

    private MatchWeights effectiveWeights(MatchWeights weights) {
        if (weights != null) {
            return weights;
        }
        return MatchWeights.builder()
                .nameWeight(BigDecimal.valueOf(0.22D))
                .pathWeight(BigDecimal.valueOf(0.22D))
                .keywordWeight(BigDecimal.valueOf(0.22D))
                .codeWeight(BigDecimal.valueOf(0.04D))
                .historyWeight(BigDecimal.valueOf(0.30D))
                .embeddingWeight(BigDecimal.ZERO)
                .build();
    }

    private double combineScores(
            MatchWeights weights,
            double scoreName,
            double scorePath,
            double scoreKeyword,
            double scoreCode,
            double scoreHistory,
            double scoreEmbedding) {
        return toDouble(weights.getNameWeight()) * scoreName
                + toDouble(weights.getPathWeight()) * scorePath
                + toDouble(weights.getKeywordWeight()) * scoreKeyword
                + toDouble(weights.getCodeWeight()) * scoreCode
                + toDouble(weights.getHistoryWeight()) * scoreHistory
                + toDouble(weights.getEmbeddingWeight()) * scoreEmbedding;
    }

    private double nameSimilarity(String leftText, String rightText, Map<String, Integer> leftCounter,
            Map<String, Integer> rightCounter) {
        double sequenceScore = MatchTextSupport.sequenceSimilarity(leftText, rightText);
        double cosineScore = MatchTextSupport.cosineSimilarity(leftCounter, rightCounter);
        return 0.55D * sequenceScore + 0.45D * cosineScore;
    }

    private double pathSimilarity(String leftText, String rightText, Map<String, Integer> leftCounter,
            Map<String, Integer> rightCounter) {
        double sequenceScore = MatchTextSupport.sequenceSimilarity(leftText, rightText);
        double cosineScore = MatchTextSupport.cosineSimilarity(leftCounter, rightCounter);
        return 0.40D * sequenceScore + 0.60D * cosineScore;
    }

    private double keywordSimilarity(String leftText, String rightText) {
        Set<String> leftKeywords = MatchTextSupport.keywordSet(leftText);
        Set<String> rightKeywords = MatchTextSupport.keywordSet(rightText);
        Set<String> leftSignals = MatchTextSupport.domainSignalSet(leftText);
        Set<String> rightSignals = MatchTextSupport.domainSignalSet(rightText);
        if (leftKeywords.isEmpty() && rightKeywords.isEmpty()) {
            return 0.5D;
        }
        if (leftKeywords.isEmpty() || rightKeywords.isEmpty()) {
            return 0D;
        }
        int overlap = MatchTextSupport.intersection(leftKeywords, rightKeywords).size();
        Set<String> unionKeywords = new HashSet<>(leftKeywords);
        unionKeywords.addAll(rightKeywords);
        int union = unionKeywords.size();
        double score = union == 0 ? 0D : (double) overlap / union;

        double conflictPenalty = 0D;
        for (List<String> conflict : List.of(
                List.of("成本", "估值增值"),
                List.of("利息", "成本"),
                List.of("应收", "应付"))) {
            String leftKeyword = conflict.get(0);
            String rightKeyword = conflict.get(1);
            if ((leftKeywords.contains(leftKeyword) && rightKeywords.contains(rightKeyword))
                    || (leftKeywords.contains(rightKeyword) && rightKeywords.contains(leftKeyword))) {
                conflictPenalty += 0.15D;
            }
        }

        Set<String> criticalTerms = Set.of(
                "成本", "估值增值", "利息", "应收", "应付", "收入", "期货", "期权", "科创板", "创业板",
                "网下", "网上", "锁定", "货币", "暂估", "信用账户", "银行间", "交易手续费", "结算服务费",
                "场外", "开放式");
        List<String> directionalTerms = List.of(
                "减值", "利息", "应收", "应付", "收入", "管理费", "托管费", "手续费", "佣金", "估值", "回购",
                "返售", "债券", "期货", "期权", "科创板", "创业板", "网下", "网上", "锁定", "信用账户", "备付金",
                "暂估", "货币", "银行间", "交易手续费", "结算服务费", "场外", "开放式");
        double directionalAdjustment = 0D;
        for (String term : directionalTerms) {
            double positiveStep = criticalTerms.contains(term) ? 0.12D : 0.08D;
            double negativeStep = criticalTerms.contains(term) ? 0.22D : 0.12D;
            if (leftSignals.contains(term) && rightSignals.contains(term)) {
                directionalAdjustment += positiveStep;
            } else if (leftSignals.contains(term) && !rightSignals.contains(term)) {
                directionalAdjustment -= negativeStep;
            }
        }
        if (!leftSignals.isEmpty() && rightSignals.containsAll(leftSignals)) {
            directionalAdjustment += 0.12D;
        }

        double signalConflictPenalty = 0D;
        for (List<String> conflict : List.of(
                List.of("期货", "期权"),
                List.of("科创板", "创业板"),
                List.of("网上", "网下"),
                List.of("锁定", "非公开发行"),
                List.of("信用账户", "备付金"),
                List.of("应收", "收入"),
                List.of("应付", "收入"))) {
            String leftSignal = conflict.get(0);
            String rightSignal = conflict.get(1);
            if ((leftSignals.contains(leftSignal) && rightSignals.contains(rightSignal))
                    || (leftSignals.contains(rightSignal) && rightSignals.contains(leftSignal))) {
                signalConflictPenalty += 0.28D;
            }
        }
        return clamp(score - conflictPenalty - signalConflictPenalty + directionalAdjustment);
    }

    private MatchRuntimeConfig runtimeConfig(MatchContext context) {
        return context == null || context.getRuntimeConfig() == null
                ? MatchRuntimeConfig.builder().embeddingStrategy("full").embeddingTopK(80).build()
                : context.getRuntimeConfig();
    }

    private boolean shouldEnableEmbedding(MatchContext context) {
        return embeddingProvider != null
                && embeddingProvider.enabled()
                && context != null
                && context.getWeights() != null
                && toDouble(context.getWeights().getEmbeddingWeight()) > 0D;
    }

    private boolean shouldEnableEmbeddingForRecall(MatchingState state) {
        return embeddingProvider != null
                && embeddingProvider.enabled()
                && state != null
                && state.standardEmbeddings() != null
                && !state.standardEmbeddings().isEmpty();
    }

    private double codeSimilarity(String leftCode, String rightCode) {
        String left = leftCode == null ? "" : leftCode.trim();
        String right = rightCode == null ? "" : rightCode.trim();
        if (left.isBlank() || right.isBlank()) {
            return 0D;
        }
        if (left.equals(right)) {
            return 1D;
        }
        String[] leftSegments = left.split("\\.");
        String[] rightSegments = right.split("\\.");
        int sharedPrefix = 0;
        for (int index = 0; index < Math.min(leftSegments.length, rightSegments.length); index++) {
            if (leftSegments[index].equals(rightSegments[index])) {
                sharedPrefix++;
            } else {
                break;
            }
        }
        double prefixRatio = (double) sharedPrefix / Math.max(leftSegments.length, rightSegments.length);
        double rootBonus = Objects.equals(extractRootKey(left), extractRootKey(right)) ? 0.25D : 0D;
        double shapeScore = 1D - (double) Math.abs(leftSegments.length - rightSegments.length)
                / Math.max(leftSegments.length, rightSegments.length);
        return Math.min(1D, 0.5D * prefixRatio + 0.25D * shapeScore + rootBonus);
    }

    private String extractRootKey(String subjectCode) {
        String code = subjectCode == null ? "" : subjectCode.trim();
        if (code.isBlank()) {
            return "";
        }
        if (code.contains(".")) {
            return code.substring(0, code.indexOf('.'));
        }
        return code.length() <= 4 ? code : code.substring(0, 4);
    }

    private ConfidenceLevel classifyConfidence(MatchCandidate best, MatchCandidate secondBest) {
        double top1 = toDouble(best.getScore());
        double top2 = secondBest == null ? 0D : toDouble(secondBest.getScore());
        double gap = top1 - top2;
        if (top1 >= 0.88D && gap >= 0.08D) {
            return ConfidenceLevel.HIGH;
        }
        if (top1 >= 0.72D && gap >= 0.04D) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }

    private int missingCriticalSignalCount(String leftText, String rightText) {
        Set<String> criticalSignals = Set.of(
                "成本", "估值增值", "货币", "利息", "期货", "期权", "科创板", "创业板",
                "网下", "网上", "锁定", "暂估", "信用账户", "银行间", "交易手续费", "结算服务费", "场外", "开放式");
        Set<String> leftSignals = new HashSet<>(MatchTextSupport.domainSignalSet(leftText));
        leftSignals.retainAll(criticalSignals);
        Set<String> rightSignals = MatchTextSupport.domainSignalSet(rightText);
        leftSignals.removeAll(rightSignals);
        return leftSignals.size();
    }

    private int extraCriticalSignalCount(String leftText, String rightText) {
        Set<String> criticalSignals = Set.of(
                "货币", "利息", "期货", "期权", "科创板", "创业板", "网下", "网上",
                "锁定", "暂估", "信用账户", "成本", "估值增值", "银行间");
        Set<String> rightSignals = new HashSet<>(MatchTextSupport.domainSignalSet(rightText));
        rightSignals.retainAll(criticalSignals);
        rightSignals.removeAll(MatchTextSupport.domainSignalSet(leftText));
        return rightSignals.size();
    }

    private double domainRuleAdjustment(String externalCode, String externalText, String standardCode,
            String standardText) {
        Set<String> externalSignals = MatchTextSupport.domainSignalSet(externalText);
        Set<String> standardSignals = MatchTextSupport.domainSignalSet(standardText);
        String externalRaw = externalText == null ? "" : externalText;
        String externalNormalized = MatchTextSupport.normalizeMatchText(externalText);
        String standardNormalized = MatchTextSupport.normalizeMatchText(standardText);
        double adjustment = 0D;

        if (externalSignals.contains("货币")) {
            adjustment += standardSignals.contains("货币") ? 0.18D : -0.10D;
        }
        if (externalNormalized.contains("定期存款")) {
            if ("1002.02.01".equals(standardCode)) {
                adjustment += 0.26D;
            }
            if (standardCode.startsWith("1002.02.01.")) {
                adjustment += 0.08D;
            }
            if (standardCode.startsWith("1002.01")) {
                adjustment -= 0.28D;
            }
        }
        if (externalNormalized.contains("定期存款") && externalNormalized.contains("中资银行")) {
            if ("1002.02.01".equals(standardCode)) {
                adjustment += 0.18D;
            }
            if (standardNormalized.contains("托管账户")) {
                adjustment -= 0.24D;
            }
        }
        if (externalSignals.contains("货币") && externalSignals.contains("成本")) {
            if (!externalSignals.contains("场外") && !externalSignals.contains("上交所")
                    && !externalSignals.contains("深交所")) {
                if (standardSignals.contains("场外") && standardSignals.contains("开放式")) {
                    adjustment += 0.18D;
                }
                if (standardSignals.contains("上交所") || standardSignals.contains("深交所")) {
                    adjustment -= 0.12D;
                }
            }
        }
        if (externalNormalized.contains("基金红利")) {
            if (standardSignals.contains("场外") && standardSignals.contains("开放式")) {
                adjustment += 0.08D;
            }
            if (externalSignals.contains("货币") && standardSignals.contains("货币")) {
                adjustment += 0.12D;
            }
        }
        if (externalRaw.contains("货币基金")) {
            if ("1203.03.27".equals(standardCode)) {
                adjustment += 0.08D;
            }
            if ("1203.03.17".equals(standardCode)) {
                adjustment -= 0.08D;
            }
        }
        if (externalSignals.contains("结算服务费") && standardSignals.contains("结算服务费")) {
            adjustment += 0.12D;
        }
        if (externalSignals.contains("交易手续费")) {
            if (standardSignals.contains("交易手续费")) {
                adjustment += 0.20D;
            }
            if (standardSignals.contains("结算服务费")) {
                adjustment -= 0.24D;
            }
        }
        if (externalSignals.contains("结算服务费") && standardSignals.contains("交易手续费")) {
            adjustment -= 0.18D;
        }
        if (externalSignals.contains("银行间") && standardSignals.contains("结算服务费") && standardSignals.contains("债券")) {
            adjustment += 0.18D;
        }
        if (externalSignals.contains("银行间") && "2209.02.01.02".equals(standardCode)) {
            adjustment += 0.24D;
        }
        if ("应付银行间交易费用".equals(externalNormalized)) {
            if ("2209.02".equals(standardCode)) {
                adjustment += 0.30D;
            } else if (Set.of("2209.02.01", "2209.02.02").contains(standardCode)) {
                adjustment += 0.18D;
            } else if (standardCode.startsWith("2209.02.01.") || standardCode.startsWith("2209.02.02.")) {
                adjustment -= 0.12D;
            }
        }
        if (Set.of("中债登手续费", "上清所手续费").contains(externalNormalized)) {
            if (Set.of("2209.02.01", "2209.02.02").contains(standardCode)) {
                adjustment += 0.18D;
            } else if ("2209.02".equals(standardCode)) {
                adjustment += 0.10D;
            } else if (standardCode.startsWith("2209.02.01.") || standardCode.startsWith("2209.02.02.")) {
                adjustment -= 0.10D;
            }
        }
        if (externalSignals.contains("上清所") && externalNormalized.contains("手续费")
                && !externalSignals.contains("结算服务费")) {
            if ("2209.02.01".equals(standardCode)) {
                adjustment += 0.08D;
            }
            if ("2209.02.01.04".equals(standardCode)) {
                adjustment += 0.05D;
            }
        }
        if ("2209.02.01".equals(standardCode)
                && !Set.of("中债登手续费", "上清所手续费", "应付银行间交易费用").contains(externalNormalized)) {
            adjustment -= 0.32D;
        }
        if ((externalCode == null ? "" : externalCode).startsWith("220981") && "2209.02.01.02".equals(standardCode)) {
            adjustment += 0.12D;
        }
        if ((externalCode == null ? "" : externalCode).startsWith("220981") && "2209.02.01".equals(standardCode)) {
            adjustment -= 0.12D;
        }
        if ((externalCode == null ? "" : externalCode).startsWith("222108")) {
            if (standardCode.startsWith("2222.07")) {
                adjustment += 0.16D;
            }
            if (standardCode.startsWith("2222.06")) {
                adjustment -= 0.10D;
            }
        }
        if (externalSignals.contains("备付金") && externalSignals.contains("利息")) {
            if (standardNormalized.contains("应收") && standardSignals.contains("利息")) {
                adjustment += 0.12D;
            }
            if (standardNormalized.contains("收入") && standardSignals.contains("利息")) {
                adjustment -= 0.12D;
            }
        }
        if (externalNormalized.contains("申购")) {
            if (standardNormalized.contains("申购")) {
                adjustment += 0.22D;
            } else if (standardNormalized.contains("清算资金往来")) {
                adjustment -= 0.08D;
            }
        }
        if (externalSignals.contains("锁定")) {
            adjustment += standardSignals.contains("锁定") ? 0.22D : -0.12D;
        }
        if (externalSignals.contains("网上") && externalSignals.contains("科创板")
                && standardNormalized.contains("新股") && standardSignals.contains("科创板")) {
            adjustment += 0.16D;
        }
        if (externalSignals.contains("网下") && externalSignals.contains("科创板")
                && standardSignals.contains("网下") && standardSignals.contains("科创板")) {
            adjustment += 0.12D;
        }
        if (externalSignals.contains("新股") && standardNormalized.contains("新股")) {
            adjustment += 0.08D;
        }
        return adjustment;
    }

    private double clamp(double value) {
        return Math.max(0D, Math.min(1D, value));
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    private String formatScore(double value) {
        return formatScore(value, 3);
    }

    private String formatScore(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private record MatchingState(
            List<StandardSubject> standardSubjects,
            Map<String, StandardSubject> standardByCode,
            Map<String, List<StandardSubject>> standardByRoot,
            Map<String, Set<String>> standardCodesByKeyword,
            Map<String, Set<String>> standardCodesByNameToken,
            Map<String, Set<String>> standardCodesByPathToken,
            Map<String, Map<String, Integer>> nameCounters,
            Map<String, Map<String, Integer>> pathCounters,
            Map<String, Set<String>> keywordSets,
            Map<String, float[]> standardEmbeddings) {
    }

    private record CandidateSource(StandardSubject standardSubject, List<String> sources) {
    }

    private record AnchorSelection(SubjectRecord anchorSubject, List<String> anchorPathNames, String anchorPathText,
            String reason) {
    }

    private record HistoryScore(double score, String reason) {
    }

    private record OverrideResult(MatchCandidate candidate, String reason) {
    }
}
