package com.yss.valset.extract.support;

import com.yss.valset.common.support.ExcelParsingSupport;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.entity.TrDwdJjhzgzbPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(JjhzgzbStandardizationSupport.class);
    private static final DateTimeFormatter BASIC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter BASIC_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String DROP_REASON_SUBJECT_EMPTY = "DROP_SUBJECT_EMPTY";

    private JjhzgzbStandardizationSupport() {
    }

    public static List<TrDwdJjhzgzbPO> buildRows(ParsedValuationData standardizedValuationData, String sourceTp, String sourceSign) {
        if (standardizedValuationData == null || standardizedValuationData.getSubjects() == null || standardizedValuationData.getSubjects().isEmpty()) {
            return List.of();
        }
        List<TrDwdJjhzgzbPO> result = new ArrayList<>();
        int droppedSubjectEmpty = 0;
        for (SubjectRecord subject : standardizedValuationData.getSubjects()) {
            TrDwdJjhzgzbPO row = buildRow(subject, standardizedValuationData.getBasicInfo(), sourceTp, sourceSign);
            if (row != null) {
                result.add(row);
            } else {
                droppedSubjectEmpty++;
            }
        }
        if (droppedSubjectEmpty > 0) {
            log.warn("t_tr_jjhzgzb 落地时存在被丢弃科目行，reason={}, droppedCount={}", DROP_REASON_SUBJECT_EMPTY, droppedSubjectEmpty);
        }
        return result;
    }

    private static TrDwdJjhzgzbPO buildRow(SubjectRecord subject, Map<String, String> basicInfo, String sourceTp, String sourceSign) {
        Map<String, Object> standardValues = subject == null || subject.getStandardValues() == null
                ? Map.of()
                : new LinkedHashMap<>(subject.getStandardValues());

        TrDwdJjhzgzbPO row = new TrDwdJjhzgzbPO();
        row.setOrgCd(firstNonBlank(
                stringValue(standardValues, "org_cd"),
                lookupBasicInfo(basicInfo, "org_cd", "orgCd", "机构代码")
        ));
        row.setPdCd(firstNonBlank(
                stringValue(standardValues, "pd_cd"),
                lookupBasicInfo(basicInfo, "pd_cd", "pdCd", "产品代码")
        ));
        row.setBizDate(normalizeDateValue(firstNonBlank(
                stringValue(standardValues, "biz_date"),
                normalizeBizDate(basicInfo, "biz_date"),
                normalizeBizDate(basicInfo, "日期"),
                extractBizDate(sourceSign)
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
        row.setSourceSign(truncate(sourceSign, 300));
        row.setSn(subject.getRowDataNumber());
        row.setDataDt(normalizeDateValue(firstNonBlank(stringValue(standardValues, "data_dt"), row.getBizDate())));
        row.setIsinCd(stringValue(standardValues, "isin_cd"));
        if ((row.getSubjectCd() == null || row.getSubjectCd().isBlank())
                && (row.getSubjectNm() == null || row.getSubjectNm().isBlank())) {
            return null;
        }
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

    private static String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static BigDecimal decimalValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        return ExcelParsingSupport.normalizeNumber(value);
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
