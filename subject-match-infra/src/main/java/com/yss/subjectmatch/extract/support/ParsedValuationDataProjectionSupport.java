package com.yss.subjectmatch.extract.support;

import com.yss.subjectmatch.domain.model.*;

import java.util.*;

/**
 * 用于将解析的工作簿数据投影到派生视图中的实用程序。
 */
public final class ParsedValuationDataProjectionSupport {

    private ParsedValuationDataProjectionSupport() {
    }

    /**
     * 构建科目关系投影。
     */
    public static List<SubjectRelation> buildSubjectRelations(List<SubjectRecord> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return List.of();
        }
        Map<String, String> subjectNameMap = new LinkedHashMap<>();
        for (SubjectRecord subject : subjects) {
            subjectNameMap.put(subject.getSubjectCode(), subject.getSubjectName());
        }
        List<SubjectRelation> relations = new ArrayList<>(subjects.size());
        for (SubjectRecord subject : subjects) {
            relations.add(SubjectRelation.builder()
                    .subjectCode(subject.getSubjectCode())
                    .subjectName(subject.getSubjectName())
                    .parentCode(subject.getParentCode())
                    .parentName(subjectNameMap.get(subject.getParentCode()))
                    .level(subject.getLevel())
                    .rootCode(subject.getRootCode())
                    .segmentCount(subject.getSegmentCount())
                    .leaf(subject.getLeaf())
                    .pathCodes(subject.getPathCodes())
                    .build());
        }
        relations.sort(Comparator
                .comparing(SubjectRelation::getRootCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(SubjectRelation::getLevel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SubjectRelation::getSubjectCode, Comparator.nullsLast(String::compareTo)));
        return relations;
    }

    /**
     * 构建科目树投影。
     */
    public static List<SubjectTreeNode> buildSubjectTree(List<SubjectRecord> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return List.of();
        }
        Map<String, SubjectTreeNode> nodeMap = new LinkedHashMap<>();
        for (SubjectRecord subject : subjects) {
                nodeMap.put(subject.getSubjectCode(), SubjectTreeNode.builder()
                        .subjectCode(subject.getSubjectCode())
                        .subjectName(subject.getSubjectName())
                        .level(subject.getLevel())
                        .parentCode(subject.getParentCode())
                        .rootCode(subject.getRootCode())
                        .leaf(subject.getLeaf())
                        .children(new ArrayList<>())
                        .build());
        }

        List<SubjectTreeNode> roots = new ArrayList<>();
        for (SubjectRecord subject : subjects) {
            SubjectTreeNode node = nodeMap.get(subject.getSubjectCode());
            if (subject.getParentCode() != null && nodeMap.containsKey(subject.getParentCode())) {
                nodeMap.get(subject.getParentCode()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        roots.sort(Comparator.comparing(SubjectTreeNode::getSubjectCode, Comparator.nullsLast(String::compareTo)));
        for (SubjectTreeNode root : roots) {
            sortTree(root);
        }
        return roots;
    }

    /**
     * 为已解析的工作簿构建摘要视图。
     */
    public static WorkbookSummary buildSummary(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null) {
            return WorkbookSummary.builder().build();
        }
        List<SubjectRecord> subjects = parsedValuationData.getSubjects() == null ? List.of() : parsedValuationData.getSubjects();
        List<MetricRecord> metrics = parsedValuationData.getMetrics() == null ? List.of() : parsedValuationData.getMetrics();

        int leafCount = 0;
        int metricRowCount = 0;
        int metricDataCount = 0;
        int maxLevel = 0;
        Set<String> rootCodes = new LinkedHashSet<>();
        Map<String, Integer> duplicateCounter = new LinkedHashMap<>();
        Map<Integer, Integer> levelDistribution = new LinkedHashMap<>();

        for (SubjectRecord subject : subjects) {
            if (Boolean.TRUE.equals(subject.getLeaf())) {
                leafCount++;
            }
            if (subject.getRootCode() != null && !subject.getRootCode().isBlank()) {
                rootCodes.add(subject.getRootCode());
            }
            duplicateCounter.merge(subject.getSubjectCode(), 1, Integer::sum);
            int level = subject.getLevel() == null ? 0 : subject.getLevel();
            levelDistribution.merge(level, 1, Integer::sum);
            maxLevel = Math.max(maxLevel, level);
        }

        for (MetricRecord metric : metrics) {
            if ("metric_row".equals(metric.getMetricType())) {
                metricRowCount++;
            } else if ("metric_data".equals(metric.getMetricType())) {
                metricDataCount++;
            }
        }

        List<String> duplicateSubjectCodes = duplicateCounter.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        Map<Integer, Integer> sortedLevelDistribution = new LinkedHashMap<>();
        levelDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedLevelDistribution.put(entry.getKey(), entry.getValue()));

        return WorkbookSummary.builder()
                .fileNameOriginal(parsedValuationData.getFileNameOriginal())
                .title(parsedValuationData.getTitle())
                .sheetName(parsedValuationData.getSheetName())
                .headerRowNumber(parsedValuationData.getHeaderRowNumber())
                .dataStartRowNumber(parsedValuationData.getDataStartRowNumber())
                .basicInfo(parsedValuationData.getBasicInfo())
                .subjectCount(subjects.size())
                .leafSubjectCount(leafCount)
                .nonLeafSubjectCount(subjects.size() - leafCount)
                .metricCount(metrics.size())
                .metricRowCount(metricRowCount)
                .metricDataCount(metricDataCount)
                .rootSubjectCount(rootCodes.size())
                .maxLevel(maxLevel)
                .duplicateSubjectCodes(duplicateSubjectCodes)
                .levelDistribution(sortedLevelDistribution)
                .build();
    }
    private static void sortTree(SubjectTreeNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }
        node.getChildren().sort(Comparator.comparing(SubjectTreeNode::getSubjectCode, Comparator.nullsLast(String::compareTo)));
        for (SubjectTreeNode child : node.getChildren()) {
            sortTree(child);
        }
    }
}
