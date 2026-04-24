package com.yss.valset.transfer.domain.rule;

import com.alibaba.qlexpress4.annotation.QLFunction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.regex.Pattern;
import java.util.Locale;

/**
 * 脚本规则可用的基础函数。
 */
public class TransferRuleFunctions {

    @QLFunction({"containsIgnoreCase"})
    public boolean containsIgnoreCase(Object source, Object keyword) {
        String sourceText = normalizeKeyword(source);
        String keywordText = normalizeKeyword(keyword);
        if (sourceText.isBlank() || keywordText.isBlank()) {
            return false;
        }
        return sourceText.toLowerCase(Locale.ROOT).contains(keywordText.toLowerCase(Locale.ROOT));
    }

    public boolean matchesRegex(String source, String regex) {
        if (source == null || regex == null) {
            return false;
        }
        return Pattern.compile(regex).matcher(source).matches();
    }

    public boolean isExcel(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    public boolean isCompressed(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z");
    }

    public boolean senderInWhitelist(String sender, Collection<String> whitelist) {
        if (sender == null || whitelist == null) {
            return false;
        }
        return whitelist.stream().filter(Objects::nonNull).anyMatch(item -> sender.equalsIgnoreCase(item));
    }

    /**
     * 判断文本是否命中任意一个关键词。
     */
    public boolean containsAny(String source, String keywords) {
        return containsAny(source, parseKeywords(keywords));
    }

    /**
     * 判断文本是否命中任意一个关键词。
     */
    public boolean containsAny(Object source, Object keywords) {
        String sourceText = normalizeKeyword(source);
        if (sourceText.isBlank()) {
            return false;
        }
        if (keywords == null) {
            return false;
        }
        if (keywords instanceof Collection<?> collection) {
            return containsAny(sourceText, collection);
        }
        if (keywords.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(keywords);
            List<Object> values = new java.util.ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(java.lang.reflect.Array.get(keywords, index));
            }
            return containsAny(sourceText, values);
        }
        return containsAny(sourceText, String.valueOf(keywords));
    }

    /**
     * 判断文本是否命中任意一个关键词。
     */
    public boolean containsAny(String source, Collection<?> keywords) {
        if (source == null || source.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        for (Object keyword : keywords) {
            String candidate = normalizeKeyword(keyword);
            if (candidate.isBlank()) {
                continue;
            }
            String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
            if (normalizedSource.equals(normalizedCandidate)
                    || normalizedSource.contains(normalizedCandidate)
                    || normalizedCandidate.contains(normalizedSource)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否同时命中所有关键词。
     */
    public boolean containsAll(String source, String keywords) {
        return containsAll(source, parseKeywords(keywords));
    }

    /**
     * 判断文本是否同时命中所有关键词。
     */
    public boolean containsAll(Object source, Object keywords) {
        String sourceText = normalizeKeyword(source);
        if (sourceText.isBlank()) {
            return false;
        }
        if (keywords == null) {
            return false;
        }
        if (keywords instanceof Collection<?> collection) {
            return containsAll(sourceText, collection);
        }
        if (keywords.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(keywords);
            List<Object> values = new java.util.ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(java.lang.reflect.Array.get(keywords, index));
            }
            return containsAll(sourceText, values);
        }
        return containsAll(sourceText, String.valueOf(keywords));
    }

    /**
     * 判断文本是否同时命中所有关键词。
     */
    public boolean containsAll(String source, Collection<?> keywords) {
        if (source == null || source.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        for (Object keyword : keywords) {
            String candidate = normalizeKeyword(keyword);
            if (candidate.isBlank()) {
                continue;
            }
            String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
            if (!(normalizedSource.equals(normalizedCandidate)
                    || normalizedSource.contains(normalizedCandidate)
                    || normalizedCandidate.contains(normalizedSource))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断文本是否命中任意一个关键词，供 QLExpress 直接调用。
     */
    @QLFunction({"containsAnyText"})
    public boolean containsAnyText(Object source, Object keywords) {
        return containsAny(source, keywords);
    }

    /**
     * 判断文本是否同时命中所有关键词，供 QLExpress 直接调用。
     */
    @QLFunction({"containsAllText"})
    public boolean containsAllText(Object source, Object keywords) {
        return containsAll(source, keywords);
    }

    private List<String> parseKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Stream.of(keywords.split("[,;|\\n]"))
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String normalizeKeyword(Object keyword) {
        return keyword == null ? "" : String.valueOf(keyword).trim();
    }
}
