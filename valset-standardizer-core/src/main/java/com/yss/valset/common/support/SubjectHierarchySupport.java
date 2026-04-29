package com.yss.valset.common.support;

import com.yss.valset.domain.model.SubjectRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用于从解析的工作簿行派生科目层次结构元数据的实用程序。
 */
public final class SubjectHierarchySupport {

    private SubjectHierarchySupport() {
    }

    /**
     * 将科目代码拆分为层次结构段。
     */
    public static List<String> splitSubjectCode(String subjectCode) {
        String code = ExcelParsingSupport.normalizeText(subjectCode)
                .replace('．', '.')
                .replace('。', '.')
                .replace('　', ' ')
                .replace('\u00A0', ' ')
                .trim();
        if (code.isEmpty()) {
            return List.of();
        }
        if (code.contains(".") || code.contains(" ")) {
            String[] segments = code.split("[\\.\\s]+");
            List<String> result = new ArrayList<>(segments.length);
            for (String segment : segments) {
                if (!segment.isBlank()) {
                    result.add(segment.trim());
                }
            }
            return result;
        }
        String compact = code.replaceAll("[\\s\\.\\p{Punct}]+", "");
        if (compact.length() <= 4) {
            return List.of(compact);
        }
        String prefix = compact.substring(0, 4);
        String remainder = compact.substring(4);
        if (!remainder.chars().allMatch(Character::isDigit)) {
            return List.of(prefix, remainder);
        }
        List<String> segments = new ArrayList<>();
        segments.add(prefix);
        while (!remainder.isEmpty()) {
            if (remainder.length() <= 2) {
                segments.add(remainder);
                break;
            }
            segments.add(remainder.substring(0, 2));
            remainder = remainder.substring(2);
        }
        return segments;
    }

    /**
     * 为科目构建累积路径代码。
     */
    public static List<String> buildSubjectPathCodes(String subjectCode, List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        List<String> pathCodes = new ArrayList<>(segments.size());
        StringBuilder current = new StringBuilder();
        for (String segment : segments) {
            current.append(segment);
            pathCodes.add(current.toString());
        }
        return pathCodes;
    }

    /**
     * 沿着科目路径查找最近的现有父代码。
     */
    public static String findExistingParentCode(List<String> pathCodes, Set<String> existingCodes) {
        if (pathCodes == null || pathCodes.size() <= 1 || existingCodes == null || existingCodes.isEmpty()) {
            return null;
        }
        for (int index = pathCodes.size() - 2; index >= 0; index--) {
            String candidate = pathCodes.get(index);
            if (existingCodes.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 使用父级和叶子元数据丰富科目。
     */
    public static List<SubjectRecord> enrichSubjectHierarchy(List<SubjectRecord> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return List.of();
        }
        Set<String> existingCodes = new LinkedHashSet<>();
        for (SubjectRecord subject : subjects) {
            existingCodes.add(subject.getSubjectCode());
        }

        Set<String> parentCodes = new HashSet<>();
        for (SubjectRecord subject : subjects) {
            String parentCode = findExistingParentCode(subject.getPathCodes(), existingCodes);
            subject.setParentCode(parentCode);
            if (parentCode != null && !parentCode.isBlank()) {
                parentCodes.add(parentCode);
            }
        }

        for (SubjectRecord subject : subjects) {
            subject.setLeaf(!parentCodes.contains(subject.getSubjectCode()));
        }
        return subjects;
    }
}
