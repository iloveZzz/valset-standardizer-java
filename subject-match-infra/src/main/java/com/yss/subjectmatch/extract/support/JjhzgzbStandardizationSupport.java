package com.yss.subjectmatch.extract.support;

import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.extract.repository.entity.TrDwdJjhzgzbPO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将标准化后的估值解析结果转换为 t_tr_jjhzgzb 行。
 */
public final class JjhzgzbStandardizationSupport {

    private static final DateTimeFormatter BASIC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter BASIC_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private JjhzgzbStandardizationSupport() {
    }

    public static List<TrDwdJjhzgzbPO> buildRows(ParsedValuationData standardizedValuationData, String sourceTp) {
        if (standardizedValuationData == null || standardizedValuationData.getSubjects() == null || standardizedValuationData.getSubjects().isEmpty()) {
            return List.of();
        }
        List<TrDwdJjhzgzbPO> result = new ArrayList<>();
        for (SubjectRecord subject : standardizedValuationData.getSubjects()) {
            TrDwdJjhzgzbPO row = buildRow(subject, standardizedValuationData.getBasicInfo(), standardizedValuationData.getTitle(), sourceTp);
            if (row != null) {
                result.add(row);
            }
        }
        return result;
    }

    private static TrDwdJjhzgzbPO buildRow(SubjectRecord subject, Map<String, String> basicInfo, String title, String sourceTp) {
        Map<String, Object> standardValues = subject == null || subject.getStandardValues() == null
                ? Map.of()
                : new LinkedHashMap<>(subject.getStandardValues());
        if (standardValues.isEmpty()) {
            return null;
        }

        TrDwdJjhzgzbPO row = new TrDwdJjhzgzbPO();
        row.setOrgCd(stringValue(standardValues, "org_cd"));
        row.setPdCd(stringValue(standardValues, "pd_cd"));
        row.setBizDate(normalizeDateValue(firstNonBlank(
                stringValue(standardValues, "biz_date"),
                normalizeBizDate(basicInfo, "biz_date"),
                normalizeBizDate(basicInfo, "日期"),
                extractBizDate(title)
        )));
        row.setSubjectCd(firstNonBlank(stringValue(standardValues, "subject_cd"), subject.getSubjectCode()));
        row.setSubjectNm(firstNonBlank(stringValue(standardValues, "subject_nm"), subject.getSubjectName()));
        row.setPaSubjectCd(firstNonBlank(stringValue(standardValues, "pa_subject_cd"), subject.getParentCode()));
        row.setPaSubjectNm(stringValue(standardValues, "pa_subject_nm"));
        row.setNHldamt(decimalValue(standardValues, "n_hldamt"));
        row.setNHldcst(decimalValue(standardValues, "n_hldcst"));
        row.setNHldcstLocl(decimalValue(standardValues, "n_hldcst_locl"));
        row.setNHldmkv(decimalValue(standardValues, "n_hldmkv"));
        row.setNHldmkvLocl(decimalValue(standardValues, "n_hldmkv_locl"));
        row.setNHldvva(decimalValue(standardValues, "n_hldvva"));
        row.setNHldvvaL(decimalValue(standardValues, "n_hldvva_l"));
        row.setCcyCd(stringValue(standardValues, "ccy_cd"));
        row.setNValrate(decimalValue(standardValues, "n_valrate"));
        row.setNPriceCost(decimalValue(standardValues, "n_price_cost"));
        row.setNValprice(decimalValue(standardValues, "n_valprice"));
        row.setNCbJzBl(decimalValue(standardValues, "n_cb_jz_bl"));
        row.setNSzJzBl(decimalValue(standardValues, "n_sz_jz_bl"));
        row.setNZcBl(decimalValue(standardValues, "n_zc_bl"));
        row.setSuspInfo(stringValue(standardValues, "susp_info"));
        row.setValuatEquity(stringValue(standardValues, "valuat_equity"));
        row.setFinAttrIdD(stringValue(standardValues, "fin_attr_id_d"));
        row.setFinMktCd(stringValue(standardValues, "fin_mkt_cd"));
        row.setTimeStamp(localDateTimeValue(standardValues, "time_stamp"));
        row.setConsFloatTpCd(stringValue(standardValues, "cons_float_tp_cd"));
        row.setSourceTp(firstNonBlank(stringValue(standardValues, "source_tp"), sourceTp));
        row.setSourceSign(buildSourceSign(subject, standardValues));
        row.setSn(subject.getRowDataNumber());
        row.setDataDt(normalizeDateValue(firstNonBlank(stringValue(standardValues, "data_dt"), row.getBizDate())));
        row.setIsAudt(booleanValue(standardValues, "is_audt"));
        row.setAudtId(stringValue(standardValues, "audt_id"));
        row.setIsinCd(stringValue(standardValues, "isin_cd"));
        return row;
    }

    private static String buildSourceSign(SubjectRecord subject, Map<String, Object> standardValues) {
        List<String> items = new ArrayList<>();
        if (subject != null) {
            if (subject.getSheetName() != null && !subject.getSheetName().isBlank()) {
                items.add("sheet=" + subject.getSheetName());
            }
            if (subject.getRowDataNumber() != null) {
                items.add("row=" + subject.getRowDataNumber());
            }
        }
        if (!standardValues.isEmpty()) {
            items.add("fields=" + String.join(",", standardValues.keySet()));
        }
        String sign = String.join(";", items);
        return sign.length() > 300 ? sign.substring(0, 300) : sign;
    }

    private static String normalizeBizDate(Map<String, String> basicInfo, String key) {
        if (basicInfo == null || basicInfo.isEmpty()) {
            return null;
        }
        String raw = basicInfo.get(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String compact = raw.replaceAll("[^0-9]", "");
        if (compact.length() >= 8) {
            return compact.substring(0, 8);
        }
        return raw.trim();
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

    private static String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : ExcelParsingSupport.normalizeText(value).trim();
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

    private static BigDecimal decimalValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        return ExcelParsingSupport.normalizeNumber(value);
    }

    private static Boolean booleanValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = ExcelParsingSupport.normalizeText(value).trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return null;
        }
        if (List.of("true", "1", "y", "yes", "是").contains(text)) {
            return Boolean.TRUE;
        }
        if (List.of("false", "0", "n", "no", "否").contains(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static LocalDateTime localDateTimeValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        String text = ExcelParsingSupport.normalizeText(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // fallback
        }
        try {
            return LocalDateTime.parse(text.replace('T', ' '), BASIC_DATE_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            // fallback
        }
        try {
            return LocalDateTime.of(
                    java.time.LocalDate.parse(text.replace('/', '-').substring(0, 10), BASIC_DATE_FORMAT),
                    java.time.LocalTime.MIDNIGHT
            );
        } catch (Exception ignored) {
            return null;
        }
    }
}
