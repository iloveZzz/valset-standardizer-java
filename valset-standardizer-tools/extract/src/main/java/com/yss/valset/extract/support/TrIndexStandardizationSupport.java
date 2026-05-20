package com.yss.valset.extract.support;

import com.yss.valset.common.support.ExcelParsingSupport;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.TrIndexPO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将标准化后的估值解析结果转换为 tr_spv_index 行。
 */
public final class TrIndexStandardizationSupport {

    private TrIndexStandardizationSupport() {
    }

    public static List<TrIndexPO> buildRows(ParsedValuationData standardizedValuationData,
                                            String sourceTp,
                                            String sourceSign,
                                            ProductBusinessFields productBusinessFields) {
        if (standardizedValuationData == null || standardizedValuationData.getMetrics() == null || standardizedValuationData.getMetrics().isEmpty()) {
            return java.util.Arrays.asList();
        }
        LocalDateTime timeStamp = LocalDateTime.now();
        List<TrIndexPO> result = new ArrayList<>(standardizedValuationData.getMetrics().size());
        for (MetricRecord metric : standardizedValuationData.getMetrics()) {
            result.addAll(buildRows(metric, standardizedValuationData.getBasicInfo(), sourceTp, sourceSign, productBusinessFields, timeStamp));
        }
        return result;
    }

    private static List<TrIndexPO> buildRows(
            MetricRecord metric,
            Map<String, String> basicInfo,
            String sourceTp,
            String sourceSign,
            ProductBusinessFields productBusinessFields,
            LocalDateTime timeStamp
    ) {
        if (metric == null) {
            return java.util.Arrays.asList();
        }
        if ("metric_row".equals(metric.getMetricType())) {
            return buildMetricRowRows(metric, basicInfo, sourceTp, sourceSign, productBusinessFields, timeStamp);
        }
        TrIndexPO row = buildMetricDataRow(metric, basicInfo, sourceTp, sourceSign, productBusinessFields, timeStamp);
        return row == null ? java.util.Arrays.asList() : java.util.Arrays.asList(row);
    }

    private static List<TrIndexPO> buildMetricRowRows(
            MetricRecord metric,
            Map<String, String> basicInfo,
            String sourceTp,
            String sourceSign,
            ProductBusinessFields productBusinessFields,
            LocalDateTime timeStamp
    ) {
        if (metric.getRawValues() == null || metric.getRawValues().isEmpty()) {
            return java.util.Arrays.asList();
        }
        List<TrIndexPO> rows = new ArrayList<>();
        Set<String> indexNames = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : metric.getRawValues().entrySet()) {
            String headerPath = normalizeHeaderPath(entry.getKey());
            BigDecimal number = ExcelParsingSupport.normalizeNumber(entry.getValue());
            if (number == null || headerPath == null || headerPath.isEmpty()) {
                continue;
            }
            String metricName = firstNonBlank(metric.getMetricName(), metric.getStandardName());
            String indexName = firstNonBlank(metricName) == null ? headerPath : metricName + "|" + headerPath;
            if (!indexNames.add(indexName)) {
                continue;
            }
            rows.add(buildRow(metric,
                    basicInfo,
                    sourceTp,
                    sourceSign,
                    productBusinessFields,
                    timeStamp,
                    indexName,
                    decimalText(number)));
        }
        return rows;
    }

    private static TrIndexPO buildMetricDataRow(
            MetricRecord metric,
            Map<String, String> basicInfo,
            String sourceTp,
            String sourceSign,
            ProductBusinessFields productBusinessFields,
            LocalDateTime timeStamp
    ) {
        BigDecimal number = firstNumber(
                metric.getValue(),
                metric.getRawValues() == null ? null : metric.getRawValues().get("value"),
                metric.getStandardValueText(),
                metric.getStandardValues() == null ? null : metric.getStandardValues().get("metric_value"),
                metric.getStandardValues() == null ? null : metric.getStandardValues().get("indx_valu")
        );
        if (number == null) {
            return null;
        }
        return buildRow(metric, basicInfo, sourceTp, sourceSign, productBusinessFields, timeStamp, null, decimalText(number));
    }

    private static TrIndexPO buildRow(
            MetricRecord metric,
            Map<String, String> basicInfo,
            String sourceTp,
            String sourceSign,
            ProductBusinessFields productBusinessFields,
            LocalDateTime timeStamp,
            String indexName,
            String indexValue
    ) {
        Map<String, Object> standardValues = metric == null || metric.getStandardValues() == null
                ? java.util.Collections.emptyMap()
                : new LinkedHashMap<>(metric.getStandardValues());

        TrIndexPO row = new TrIndexPO();
        row.setOrgCd(truncate(firstNonBlank(
                productBusinessFields == null ? null : productBusinessFields.getOrgCode(),
                stringValue(standardValues, "org_cd"),
                lookupBasicInfo(basicInfo, "org_cd", "机构代码"),
                lookupBasicInfo(basicInfo, "orgCd", "机构代码")
        ), 30));
        row.setPdCd(truncate(firstNonBlank(
                productBusinessFields == null ? null : productBusinessFields.getProductCode(),
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
                indexName,
                metric == null ? null : metric.getMetricName(),
                stringValue(standardValues, "indx_nm"),
                stringValue(standardValues, "metric_name"),
                metric == null ? null : metric.getStandardName()
        ), 300));
        row.setIndxValu(truncate(firstNonBlank(
                indexValue,
                stringValue(standardValues, "indx_valu"),
                stringValue(standardValues, "metric_value"),
                metric == null ? null : metric.getStandardValueText(),
                metric == null ? null : metric.getValue()
        ), 300));
        row.setSourceTp(truncate(firstNonBlank(stringValue(standardValues, "source_tp"), sourceTp), 30));
        row.setSourceSign(truncate(sourceSign, 300));
        row.setSn(metric == null ? null : metric.getRowDataNumber());
        row.setTimeStamp(timeStamp);
        return row;
    }

    private static String lookupBasicInfo(Map<String, String> basicInfo, String... keys) {
        if (basicInfo == null || basicInfo.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            String value = basicInfo.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : ExcelParsingSupport.normalizeText(value).trim();
    }

    private static String normalizeHeaderPath(String headerPath) {
        if (headerPath == null || headerPath.trim().isEmpty()) {
            return null;
        }
        String[] segments = headerPath.split("\\|");
        Set<String> uniqueSegments = new LinkedHashSet<>();
        for (String segment : segments) {
            if (segment == null || segment.trim().isEmpty()) {
                continue;
            }
            uniqueSegments.add(segment.trim());
        }
        return uniqueSegments.isEmpty() ? null : String.join("|", uniqueSegments);
    }

    private static String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal firstNumber(Object... candidates) {
        if (candidates == null) {
            return null;
        }
        for (Object candidate : candidates) {
            BigDecimal number = ExcelParsingSupport.normalizeNumber(candidate);
            if (number != null) {
                return number;
            }
        }
        return null;
    }

    private static String normalizeDateValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String compact = text.replaceAll("[^0-9]", "");
        if (compact.length() >= 8) {
            return compact.substring(0, 8);
        }
        return text.trim();
    }

    private static String extractBizDate(String text) {
        if (text == null || text.trim().isEmpty()) {
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
            if (candidate != null && !candidate.trim().isEmpty()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
