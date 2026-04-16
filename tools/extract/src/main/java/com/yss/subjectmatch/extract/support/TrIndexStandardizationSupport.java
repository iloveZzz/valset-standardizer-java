package com.yss.subjectmatch.extract.support;

import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.repository.entity.TrIndexPO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将标准化后的估值解析结果转换为 t_tr_index 行。
 */
public final class TrIndexStandardizationSupport {

    private TrIndexStandardizationSupport() {
    }

    public static List<TrIndexPO> buildRows(ParsedValuationData standardizedValuationData, String sourceTp, String sourceSign) {
        if (standardizedValuationData == null || standardizedValuationData.getMetrics() == null || standardizedValuationData.getMetrics().isEmpty()) {
            return List.of();
        }
        LocalDateTime timeStamp = LocalDateTime.now();
        List<TrIndexPO> result = new ArrayList<>(standardizedValuationData.getMetrics().size());
        for (MetricRecord metric : standardizedValuationData.getMetrics()) {
            TrIndexPO row = buildRow(metric, standardizedValuationData.getBasicInfo(), sourceTp, sourceSign, timeStamp);
            if (row != null) {
                result.add(row);
            }
        }
        return result;
    }

    private static TrIndexPO buildRow(
            MetricRecord metric,
            Map<String, String> basicInfo,
            String sourceTp,
            String sourceSign,
            LocalDateTime timeStamp
    ) {
        Map<String, Object> standardValues = metric == null || metric.getStandardValues() == null
                ? Map.of()
                : new LinkedHashMap<>(metric.getStandardValues());

        TrIndexPO row = new TrIndexPO();
        row.setOrgCd(truncate(firstNonBlank(
                stringValue(standardValues, "org_cd"),
                lookupBasicInfo(basicInfo, "org_cd", "机构代码"),
                lookupBasicInfo(basicInfo, "orgCd", "机构代码")
        ), 30));
        row.setPdCd(truncate(firstNonBlank(
                stringValue(standardValues, "pd_cd"),
                lookupBasicInfo(basicInfo, "pd_cd", "产品代码"),
                lookupBasicInfo(basicInfo, "pdCd", "产品代码")
        ), 60));
        row.setBizDate(truncate(normalizeDateValue(firstNonBlank(
                stringValue(standardValues, "biz_date"),
                lookupBasicInfo(basicInfo, "biz_date", "业务日期", "日期"),
                lookupBasicInfo(basicInfo, "bizDate", "业务日期", "日期"),
                extractBizDate(sourceSign)
        )), 8));
        row.setIndxNm(truncate(firstNonBlank(
                stringValue(standardValues, "indx_nm"),
                stringValue(standardValues, "metric_name"),
                metric == null ? null : metric.getStandardName(),
                metric == null ? null : metric.getMetricName()
        ), 300));
        row.setIndxValu(truncate(firstNonBlank(
                stringValue(standardValues, "indx_valu"),
                stringValue(standardValues, "metric_value"),
                metric == null ? null : metric.getStandardValueText(),
                metric == null ? null : metric.getValue()
        ), 300));
        row.setSourceTp(truncate(firstNonBlank(stringValue(standardValues, "source_tp"), sourceTp), 30));
        row.setSourceSign(truncate(sourceSign, 300));
        row.setTimeStamp(timeStamp);
        return row;
    }

    private static String lookupBasicInfo(Map<String, String> basicInfo, String... keys) {
        if (basicInfo == null || basicInfo.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = basicInfo.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : ExcelParsingSupport.normalizeText(value).trim();
    }

    private static String normalizeDateValue(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String compact = text.replaceAll("[^0-9]", "");
        if (compact.length() >= 8) {
            return compact.substring(0, 8);
        }
        return text.trim();
    }

    private static String extractBizDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String compact = text.replaceAll("[^0-9]", "");
        if (compact.length() < 8) {
            return null;
        }
        return compact.substring(compact.length() - 8);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
