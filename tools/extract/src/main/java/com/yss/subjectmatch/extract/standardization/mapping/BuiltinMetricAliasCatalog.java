package com.yss.subjectmatch.extract.standardization.mapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部估值指标内置别名白名单。
 */
public final class BuiltinMetricAliasCatalog {

    private static final Map<String, String> ALIAS_TO_STANDARD_NAME;

    static {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("资产合计", "资产合计");
        mapping.put("负债合计", "负债合计");
        mapping.put("资产净值", "资产净值");
        mapping.put("单位净值", "单位净值");
        mapping.put("今日单位净值", "单位净值");
        mapping.put("T+1日份额参考净值", "T+1日份额参考净值");
        mapping.put("本日收益", "本日收益");
        mapping.put("期间累计本日收益", "期间累计本日收益");
        mapping.put("每万份收益", "每万份收益");
        mapping.put("每日万份收益", "每万份收益");
        mapping.put("每万份本年累计收益", "每万份本年累计收益");
        mapping.put("期间累计万份收益", "期间累计万份收益");
        mapping.put("基金累计收益", "基金累计收益");
        mapping.put("基金累计万份收益", "基金累计万份收益");
        mapping.put("七日年化收益率", "七日年化收益率");
        mapping.put("三十日年化收益率", "三十日年化收益率");
        mapping.put("六十日年化收益率", "六十日年化收益率");
        mapping.put("九十日年化收益率", "九十日年化收益率");
        mapping.put("偏离度", "偏离度");
        mapping.put("偏离金额", "偏离金额");
        mapping.put("证券投资合计", "证券投资合计");
        mapping.put("实收资本", "实收资本");
        mapping.put("净值(成本)", "净值(成本)");
        mapping.put("净值 (成本)", "净值(成本)");
        ALIAS_TO_STANDARD_NAME = Map.copyOf(mapping);
    }

    private BuiltinMetricAliasCatalog() {
    }

    public static BuiltinMetricMapping match(String metricName) {
        if (metricName == null || metricName.isBlank()) {
            return null;
        }
        String candidateText = canonicalize(metricName);
        String bestAlias = null;
        String bestStandardName = null;
        for (Map.Entry<String, String> entry : ALIAS_TO_STANDARD_NAME.entrySet()) {
            String alias = entry.getKey();
            if (alias == null || alias.isBlank() || alias.length() < 2) {
                continue;
            }
            String normalizedAlias = canonicalize(alias);
            if (!candidateText.contains(normalizedAlias)) {
                continue;
            }
            if (bestAlias == null || normalizedAlias.length() > bestAlias.length()) {
                bestAlias = normalizedAlias;
                bestStandardName = entry.getValue();
            }
        }
        if (bestStandardName == null || bestStandardName.isBlank()) {
            return null;
        }
        return new BuiltinMetricMapping(bestStandardName, bestStandardName);
    }

    private static String canonicalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "");
    }

    public record BuiltinMetricMapping(String standardCode, String standardName) {
    }
}
