package com.yss.valset.analysis.support;

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
        List<String> segments = new ArrayList<>();
        segments.add(compact.substring(0, 4));
        String remainder = compact.substring(4);
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
