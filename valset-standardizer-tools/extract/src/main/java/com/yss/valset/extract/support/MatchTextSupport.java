package com.yss.valset.extract.support;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 匹配器使用的文本规范化和相似性助手。
 */
public final class MatchTextSupport {

    private static final Pattern INSTANCE_PATTERN_DIGITS = Pattern.compile("\\d{6,}");
    private static final Pattern INSTANCE_PATTERN_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern INSTANCE_PATTERN_SUFFIX = Pattern.compile("\\(\\d{8}\\)");
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[_\\-\\s>/]+");
    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\w\\u4e00-\\u9fff]+");

    private static final Set<String> INSTANCE_KEYWORDS = Set.of(
            "支行", "营业部", "席位", "账户", "账户号", "证券", "基金", "公司", "银行", "银行股份", "托管行", "代码", "编号", "otc", "ib"
    );

    private static final Set<String> BUSINESS_KEYWORDS = Set.of(
            "存款", "协议存款", "活期", "定期", "通知存款", "回购", "返售", "利息", "应计", "成本", "估值", "增值", "减值", "准备",
            "应收", "应付", "收入", "管理费", "托管费", "手续费", "佣金", "债券", "股票", "新股", "未上市", "网上", "网下", "锁定",
            "限售", "科创板", "创业板", "银行间", "上交所", "深交所", "场外", "开放式", "中债登", "上清所", "质押式", "买断式",
            "期货", "期权", "备付金", "清算备付金", "信用账户", "普通账户", "货币", "暂估", "增值税", "城建税", "教育税",
            "教育附加", "地方教育附加", "印花税", "结算服务费", "结算费用", "交易费用", "交易手续费"
    );

    private static final Set<String> GENERIC_TOKENS = Set.of("资产", "金融", "产品", "集合", "专用", "默认", "其他");

    private static final Map<String, String> SYNONYM_GROUPS;

    private static final Set<String> DOMAIN_SIGNALS = Set.of(
            "成本", "估值增值", "期货", "期权", "科创板", "创业板", "网上", "网下", "锁定", "新股", "非公开发行", "信用账户",
            "备付金", "利息", "应收", "应付", "收入", "暂估", "货币", "场外", "开放式", "上交所", "深交所", "银行间",
            "债券", "结算服务费", "交易手续费", "城建税", "教育税", "教育附加"
    );

    static {
        Map<String, String> synonymGroups = new LinkedHashMap<>();
        synonymGroups.put("应计利息", "利息");
        synonymGroups.put("浮盈", "估值增值");
        synonymGroups.put("管理费", "基金管理费");
        synonymGroups.put("托管费", "基金托管费");
        synonymGroups.put("银行存款_活期", "活期存款");
        synonymGroups.put("银行存款_定期", "定期存款");
        synonymGroups.put("科创版", "科创板");
        synonymGroups.put("新股限售", "新股锁定");
        synonymGroups.put("限售", "锁定");
        synonymGroups.put("城市维护建设税", "城建税");
        synonymGroups.put("教育费附加", "教育税");
        synonymGroups.put("地方教育费附加", "地方教育附加");
        synonymGroups.put("应交税费", "应付税费");
        synonymGroups.put("结算费用", "结算服务费");
        synonymGroups.put("货币基金", "货币");
        synonymGroups.put("期货备付金", "期货清算备付金");
        synonymGroups.put("存出期货保证金", "期货交易保证金");
        synonymGroups.put("期货保证金", "期货交易保证金");
        SYNONYM_GROUPS = Collections.unmodifiableMap(synonymGroups);
    }

    private MatchTextSupport() {
    }

    /**
     * 规范化一段文本以进行匹配。
     */
    public static String normalizeMatchText(String value) {
        String text = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).trim();
        text = text.replace('（', '(').replace('）', ')');
        text = text.replace('－', '-').replace('—', '-');
        text = text.replace('／', '/');
        text = text.replace(">", " ");
        text = text.replace('：', ':');
        String lowered = text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : SYNONYM_GROUPS.entrySet()) {
            lowered = lowered.replace(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().toLowerCase(Locale.ROOT));
        }
        lowered = NON_WORD_PATTERN.matcher(lowered).replaceAll(" ");
        return lowered.replaceAll("\\s+", " ").trim();
    }

    /**
     * 将规范化文本标记为可匹配的片段。
     */
    public static List<String> tokenizeText(String value) {
        String normalized = normalizeMatchText(value);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<String> rawTokens = new ArrayList<>();
        for (String chunk : normalized.split("\\s+")) {
            if (chunk.isBlank()) {
                continue;
            }
            String[] tokens = TOKEN_SPLIT_PATTERN.split(chunk);
            for (String token : tokens) {
                if (!token.isBlank()) {
                    rawTokens.add(token);
                }
            }
        }

        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (token.length() == 1 && !BUSINESS_KEYWORDS.contains(token)) {
                continue;
            }
            if (GENERIC_TOKENS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    /**
     * 从层次结构名称构建可显示的路径字符串。
     */
    public static String buildPathText(List<String> pathNames) {
        if (pathNames == null || pathNames.isEmpty()) {
            return "";
        }
        List<String> filtered = new ArrayList<>();
        for (String pathName : pathNames) {
            if (pathName != null && !pathName.isBlank()) {
                filtered.add(pathName);
            }
        }
        return String.join(" > ", filtered);
    }

    /**
     * 计算文本值中的标记。
     */
    public static Map<String, Integer> tokenCounter(String value) {
        List<String> tokens = tokenizeText(value);
        if (tokens.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> counter = new HashMap<>();
        for (String token : tokens) {
            counter.merge(token, 1, Integer::sum);
        }
        return counter;
    }

    /**
     * 计算两个标记计数图之间的余弦相似度。
     */
    public static double cosineSimilarity(Map<String, Integer> left, Map<String, Integer> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0D;
        }
        double dot = 0D;
        for (Map.Entry<String, Integer> entry : left.entrySet()) {
            dot += entry.getValue() * right.getOrDefault(entry.getKey(), 0);
        }
        double leftNorm = 0D;
        for (Integer value : left.values()) {
            leftNorm += value * value;
        }
        double rightNorm = 0D;
        for (Integer value : right.values()) {
            rightNorm += value * value;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /**
     * 从文本构建标记集。
     */
    public static Set<String> keywordSet(String value) {
        String normalized = normalizeMatchText(value);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String keyword : BUSINESS_KEYWORDS) {
            if (normalized.contains(keyword)) {
                result.add(keyword);
            }
        }
        return result;
    }

    /**
     * 从文本构建域信号集。
     */
    public static Set<String> domainSignalSet(String value) {
        String normalized = normalizeMatchText(value);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String signal : DOMAIN_SIGNALS) {
            if (normalized.contains(signal)) {
                result.add(signal);
            }
        }
        return result;
    }

    /**
     * 检测类似占位符的科目。
     */
    public static boolean isPlaceholderSubject(String code, String name) {
        return (code != null && code.contains("<")) || (name != null && name.contains("<"));
    }

    /**
     * 检测类似实例的外部名称。
     */
    public static boolean isInstanceLikeName(String name) {
        String normalized = normalizeMatchText(name);
        if (normalized.isEmpty()) {
            return false;
        }
        if (INSTANCE_PATTERN_DIGITS.matcher(normalized).find()
                || INSTANCE_PATTERN_DATE.matcher(normalized).find()
                || INSTANCE_PATTERN_SUFFIX.matcher(normalized).find()) {
            return true;
        }
        if (normalized.matches("[a-z0-9._ ]+")) {
            return true;
        }
        if (containsAny(normalized, INSTANCE_KEYWORDS) && !containsAny(normalized, BUSINESS_KEYWORDS)) {
            return true;
        }
        return keywordSet(normalized).isEmpty() && tokenizeText(normalized).size() <= 2;
    }

    /**
     * 检测类似业务的外部名称。
     */
    public static boolean isBusinessLikeName(String name) {
        String normalized = normalizeMatchText(name);
        if (normalized.isEmpty()) {
            return false;
        }
        if (containsAny(normalized, BUSINESS_KEYWORDS)) {
            return true;
        }
        List<String> tokens = tokenizeText(normalized);
        return tokens.size() >= 2 && !isInstanceLikeName(name);
    }

    /**
     * 检查文本是否包含任何提供的关键字。
     */
    public static boolean containsAny(String text, Collection<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算两个字符串的序列式相似度得分。
     */
    public static double sequenceSimilarity(String left, String right) {
        String normalizedLeft = normalizeMatchText(left);
        String normalizedRight = normalizeMatchText(right);
        if (normalizedLeft.isEmpty() && normalizedRight.isEmpty()) {
            return 1D;
        }
        if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) {
            return 0D;
        }
        int leftLength = normalizedLeft.length();
        int rightLength = normalizedRight.length();
        int[][] distance = new int[leftLength + 1][rightLength + 1];
        for (int i = 0; i <= leftLength; i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= rightLength; j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= leftLength; i++) {
            for (int j = 1; j <= rightLength; j++) {
                int cost = normalizedLeft.charAt(i - 1) == normalizedRight.charAt(j - 1) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost
                );
            }
        }
        int maxLength = Math.max(leftLength, rightLength);
        return maxLength == 0 ? 1D : Math.max(0D, 1D - (double) distance[leftLength][rightLength] / maxLength);
    }

    /**
     * 计算两个集合的交集。
     */
    public static Set<String> intersection(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>(left);
        result.retainAll(right);
        return result;
    }
}
