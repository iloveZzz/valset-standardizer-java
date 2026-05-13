package com.yss.valset.extract.standardization.mapping;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 表头映射规则共享助手。
 */
public final class HeaderMappingRuleSupport {

    private HeaderMappingRuleSupport() {
    }

    /**
     * 规范化分段列表。
     */
    public static List<String> normalizeSegments(List<String> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(segments.size());
        for (String segment : segments) {
            if (segment == null) {
                continue;
            }
            String text = segment.trim();
            if (!text.isBlank()) {
                normalized.add(text);
            }
        }
        return List.copyOf(normalized);
    }

    /**
     * 解析精确表头候选。
     */
    public static ResolvedHeaderCandidate resolveExactCandidate(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null || lookup == null || input.headerText() == null || input.headerText().isBlank()) {
            return null;
        }
        String headerText = input.headerText().trim();
        HeaderMappingCandidate candidate = lookup.findExact(headerText);
        if (candidate == null) {
            return null;
        }
        return new ResolvedHeaderCandidate(candidate, headerText);
    }

    /**
     * 解析分段表头候选。
     */
    public static ResolvedHeaderCandidate resolveSegmentCandidate(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null || lookup == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        if (input.headerText() != null && !input.headerText().isBlank()) {
            for (String segment : input.headerText().split("\\|")) {
                if (segment != null && !segment.isBlank()) {
                    candidates.add(segment.trim());
                }
            }
        }
        if (input.segments() != null) {
            candidates.addAll(normalizeSegments(input.segments()));
        }
        for (String segment : candidates) {
            HeaderMappingCandidate candidate = lookup.findExact(segment);
            if (candidate != null) {
                return new ResolvedHeaderCandidate(candidate, segment);
            }
        }
        return null;
    }

    /**
     * 解析别名候选。
     */
    public static ResolvedHeaderCandidate resolveAliasCandidate(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null || lookup == null || input.headerText() == null || input.headerText().isBlank()) {
            return null;
        }
        String headerText = input.headerText().trim();
        HeaderMappingCandidate candidate = lookup.findAliasContains(headerText);
        if (candidate == null) {
            return null;
        }
        return new ResolvedHeaderCandidate(candidate, headerText);
    }

    /**
     * 判断表头文本是否命中任意分段关键词。
     */
    public static boolean headerContainsAnySegment(String headerText, List<String> keywords) {
        if (headerText == null || headerText.isBlank() || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String segment : normalizeSegments(keywords)) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (headerText.contains(segment) || segment.contains(headerText.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断表头文本是否同时命中全部分段关键词。
     */
    public static boolean headerContainsAllSegments(String headerText, List<String> keywords) {
        if (headerText == null || headerText.isBlank() || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String segment : normalizeSegments(keywords)) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (!(headerText.contains(segment) || segment.contains(headerText.trim()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 已解析的候选对象。
     */
    public record ResolvedHeaderCandidate(HeaderMappingCandidate candidate, String matchedText) {
    }
}
