package com.yss.valset.qlexpress.domain.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 无数据库环境下的系统函数种子脚本快照。
 */
public final class SystemQlexpressFunctionSeedScripts {

    public static final String COMMON_SCOPE = "common";
    public static final String EXTRACT_PARSE_SCOPE = "extract.parse";
    public static final String EXTRACT_HEADER_MAPPING_SCOPE = "extract.headerMapping";
    public static final String TRANSFER_RULE_SCOPE = "transfer.rule";

    private static final List<QlexpressFunctionScript> SCRIPTS = Collections.unmodifiableList(Arrays.asList(
            script("rowContainsAll", "function rowContainsAll(row, keywords) { if (keywords == null || keywords.length == 0) { return false; }; return rowHitCount(row, keywords) >= keywords.length; }", EXTRACT_PARSE_SCOPE),
            script("rowContainsAny", "function rowContainsAny(row, keywords) { if (row == null || keywords == null) { return false; }; for (keyword : keywords) { if (keyword == null) { continue; }; kw = \"\" + keyword; if (kw == \"\") { continue; }; for (cell : row) { if (matchesKeyword(cell, kw)) { return true; }; }; }; return false; }", EXTRACT_PARSE_SCOPE),
            script("containsAny", "function containsAny(source, keywords) { if (source == null || keywords == null) { return false; }; text = \"\" + source; if (text == \"\") { return false; }; for (keyword : keywords) { if (matchesKeyword(text, keyword)) { return true; }; }; return false; }", EXTRACT_PARSE_SCOPE),
            script("containsAll", "function containsAll(source, keywords) { if (source == null || keywords == null || keywords.length == 0) { return false; }; text = \"\" + source; if (text == \"\") { return false; }; for (keyword : keywords) { if (!matchesKeyword(text, keyword)) { return false; }; }; return true; }", EXTRACT_PARSE_SCOPE),
            script("hasText", "function hasText(value) { return value != null && (\"\" + value) != \"\"; }", COMMON_SCOPE),
            script("regexMatches", "function regexMatches(source, regex) { return qlCommonFns.matchesRegex(source, regex); }", COMMON_SCOPE),
            script("rowHitCount", "function rowHitCount(row, keywords) { if (row == null || keywords == null) { return 0; }; hitCount = 0; for (keyword : keywords) { if (keyword == null) { continue; }; kw = \"\" + keyword; if (kw == \"\") { continue; }; matched = false; for (cell : row) { if (matchesKeyword(cell, kw)) { matched = true; break; }; }; if (matched) { hitCount = hitCount + 1; }; }; return hitCount; }", EXTRACT_PARSE_SCOPE),
            script("matchesKeyword", "function matchesKeyword(source, keyword) { if (source == null || keyword == null) { return false; }; text = \"\" + source; kw = \"\" + keyword; if (text == \"\" || kw == \"\") { return false; }; return text == kw || kw in text || text in kw; }", EXTRACT_PARSE_SCOPE),
            script("isHeaderRow", "function isHeaderRow(row, requiredHeaders) { return rowContainsAll(row, requiredHeaders); }", EXTRACT_PARSE_SCOPE),
            script("isDataStartRow", "function isDataStartRow(row) { return isDataStartRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
            script("isDataStartRowWithPattern", "function isDataStartRowWithPattern(row, pattern) { return isSubjectRowWithPattern(row, pattern) || isMetricCandidateWithPattern(row, pattern); }", EXTRACT_PARSE_SCOPE),
            script("isSubjectRow", "function isSubjectRow(row) { return isSubjectRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
            script("isSubjectRowWithPattern", "function isSubjectRowWithPattern(row, pattern) { if (row == null) { return false; }; for (i = 0; i < row.length; i++) { if (isSubjectCodeText(row[i], pattern)) { return hasSubjectNameAfterCode(row, i); }; }; return false; }", EXTRACT_PARSE_SCOPE),
            script("isSubjectCodeText", "function isSubjectCodeText(value, pattern) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\") { return false; }; if (regexMatches(text, \".*[一-龥].*\")) { return false; }; if (\"科\" in text || \"目\" in text || \"名\" in text || \"称\" in text || \"：\" in text || \":\" in text || \" \" in text || \",\" in text || \"，\" in text) { return false; }; subjectCodePattern = pattern == null || pattern == \"\" ? \"^\\\\d{4}[A-Za-z0-9]*\\$\" : pattern; return regexMatches(text, subjectCodePattern); }", EXTRACT_PARSE_SCOPE),
            script("hasSubjectNameAfterCode", "function hasSubjectNameAfterCode(row, codeIndex) { if (row == null || codeIndex == null || codeIndex < 0) { return false; }; for (i = codeIndex + 1; i < row.length; i++) { text = textAt(row, i); if (text != \"\" && text != \"-\") { return true; }; }; return false; }", EXTRACT_PARSE_SCOPE),
            script("isMetricCandidate", "function isMetricCandidate(row) { return isMetricCandidateWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
            script("isMetricCandidateWithPattern", "function isMetricCandidateWithPattern(row, pattern) { return isMetricDataRowWithPattern(row, pattern) || isMetricRowWithPattern(row, pattern); }", EXTRACT_PARSE_SCOPE),
            script("isMetricDataRow", "function isMetricDataRow(row) { return isMetricDataRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
            script("isMetricDataRowWithPattern", "function isMetricDataRowWithPattern(row, pattern) { if (row == null || row.length < 2) { return false; }; firstCell = textAt(row, 0); if (firstCell == \"\" || isSubjectCodeText(firstCell, pattern)) { return false; }; if (textAt(row, 1) == \"\") { return false; }; for (i = 2; i < row.length; i++) { if (textAt(row, i) != \"\") { return false; }; }; return true; }", EXTRACT_PARSE_SCOPE),
            script("isMetricRow", "function isMetricRow(row) { return isMetricRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
            script("isMetricRowWithPattern", "function isMetricRowWithPattern(row, pattern) { if (row == null || row.length < 2) { return false; }; firstCell = textAt(row, 0); if (firstCell == \"\" || isSubjectCodeText(firstCell, pattern)) { return false; }; filledCount = 0; for (i = 1; i < row.length; i++) { if (textAt(row, i) != \"\") { filledCount = filledCount + 1; }; }; return filledCount >= 2; }", EXTRACT_PARSE_SCOPE),
            script("isFooterRow", "function isFooterRow(row, footerKeywords) { keywords = footerKeywords; if (keywords == null || keywords.length == 0) { keywords = [\"制表\", \"复核\", \"打印\", \"备注\"]; }; return containsAny(firstMeaningfulText(row), keywords); }", EXTRACT_PARSE_SCOPE),
            script("classifyRow", "function classifyRow(row, footerKeywords) { return classifyRowWithPattern(row, footerKeywords, null); }", EXTRACT_PARSE_SCOPE),
            script("classifyRowWithPattern", "function classifyRowWithPattern(row, footerKeywords, pattern) { if (isSubjectRowWithPattern(row, pattern)) { return \"SUBJECT\"; }; if (isMetricDataRowWithPattern(row, pattern)) { return \"METRIC_DATA\"; }; if (isMetricRowWithPattern(row, pattern)) { return \"METRIC_ROW\"; }; if (isFooterRow(row, footerKeywords)) { return \"FOOTER\"; }; return \"IGNORE\"; }", EXTRACT_PARSE_SCOPE),
            script("firstMeaningfulTextContainsAny", "function firstMeaningfulTextContainsAny(row, keywords) { return containsAny(firstMeaningfulText(row), keywords); }", EXTRACT_PARSE_SCOPE),
            script("firstMeaningfulTextContainsAll", "function firstMeaningfulTextContainsAll(row, keywords) { return containsAll(firstMeaningfulText(row), keywords); }", EXTRACT_PARSE_SCOPE),
            script("hasAtLeastNonBlank", "function hasAtLeastNonBlank(row, minCount) { return minCount != null && minCount > 0 && rowNonBlankCount(row) >= minCount; }", EXTRACT_PARSE_SCOPE),
            script("textAt", "function textAt(row, index) { if (row == null || index == null || index < 0 || index >= row.length || row[index] == null) { return \"\"; }; return \"\" + row[index]; }", EXTRACT_PARSE_SCOPE),
            script("valueAt", "function valueAt(row, index) { if (row == null || index == null || index < 0 || index >= row.length) { return null; }; return row[index]; }", EXTRACT_PARSE_SCOPE),
            script("rowNonBlankCount", "function rowNonBlankCount(row) { if (row == null) { return 0; }; count = 0; for (cell : row) { text = cell == null ? \"\" : \"\" + cell; if (text != \"\" && text != \"-\") { count = count + 1; }; }; return count; }", EXTRACT_PARSE_SCOPE),
            script("firstMeaningfulText", "function firstMeaningfulText(row) { if (row == null) { return \"\"; }; for (cell : row) { text = cell == null ? \"\" : \"\" + cell; if (text != \"\" && text != \"-\") { return text; }; }; return \"\"; }", EXTRACT_PARSE_SCOPE),
            script("newMap", "function newMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10) { return mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10); }", EXTRACT_PARSE_SCOPE),
            script("mapOf", "function mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10) { m = {:}; if (k1 != null) { m[k1] = v1; }; if (k2 != null) { m[k2] = v2; }; if (k3 != null) { m[k3] = v3; }; if (k4 != null) { m[k4] = v4; }; if (k5 != null) { m[k5] = v5; }; if (k6 != null) { m[k6] = v6; }; if (k7 != null) { m[k7] = v7; }; if (k8 != null) { m[k8] = v8; }; if (k9 != null) { m[k9] = v9; }; if (k10 != null) { m[k10] = v10; }; return m; }", EXTRACT_PARSE_SCOPE),
            script("put", "function put(map, key, value) { result = map == null ? {:} : map; if (key != null) { result[key] = value; }; return result; }", EXTRACT_PARSE_SCOPE),
            script("hasCandidate", "function hasCandidate(candidate) { return qlHeaderFns.hasCandidate(candidate); }", EXTRACT_HEADER_MAPPING_SCOPE),
            script("headerContainsAnySegment", "function headerContainsAnySegment(headerText, segments) { return qlHeaderFns.headerContainsAnySegment(headerText, segments); }", EXTRACT_HEADER_MAPPING_SCOPE),
            script("headerContainsAllSegments", "function headerContainsAllSegments(headerText, segments) { return qlHeaderFns.headerContainsAllSegments(headerText, segments); }", EXTRACT_HEADER_MAPPING_SCOPE),
            script("transferText", "function transferText(value) { return value == null ? \"\" : \"\" + value; }", TRANSFER_RULE_SCOPE),
            script("textMatches", "function textMatches(source, keyword) { text = transferText(source); kw = transferText(keyword); if (text == \"\" || kw == \"\") { return false; }; return text == kw || kw in text || text in kw || text like kw; }", TRANSFER_RULE_SCOPE),
            script("containsIgnoreCase", "function containsIgnoreCase(source, keyword) { return textMatches(source, keyword); }", TRANSFER_RULE_SCOPE),
            script("matchesRegex", "function matchesRegex(source, regex) { return textMatches(source, regex); }", TRANSFER_RULE_SCOPE),
            script("matchesAnyRegex", "function matchesAnyRegex(source, regexList) { return containsAnyText(source, regexList); }", TRANSFER_RULE_SCOPE),
            script("containsAnyText", "function containsAnyText(source, keywords) { if (source == null || keywords == null) { return false; }; for (keyword : keywords) { if (textMatches(source, keyword)) { return true; }; }; return false; }", TRANSFER_RULE_SCOPE),
            script("containsAllText", "function containsAllText(source, keywords) { if (source == null || keywords == null || keywords.length == 0) { return false; }; for (keyword : keywords) { if (!textMatches(source, keyword)) { return false; }; }; return true; }", TRANSFER_RULE_SCOPE),
            script("isExcel", "function isExcel(fileName) { text = transferText(fileName); return text like \"%.xlsx\" || text like \"%.xls\" || text like \"%.XLSX\" || text like \"%.XLS\"; }", TRANSFER_RULE_SCOPE),
            script("isExcelFile", "function isExcelFile(fileName) { return isExcel(fileName); }", TRANSFER_RULE_SCOPE),
            script("isCsv", "function isCsv(fileName) { text = transferText(fileName); return text like \"%.csv\" || text like \"%.CSV\"; }", TRANSFER_RULE_SCOPE),
            script("isCsvFile", "function isCsvFile(fileName) { return isCsv(fileName); }", TRANSFER_RULE_SCOPE),
            script("readExcelData", "function readExcelData(source) { return source == null ? [] : source; }", TRANSFER_RULE_SCOPE),
            script("readCsvData", "function readCsvData(source) { return source == null ? [] : source; }", TRANSFER_RULE_SCOPE),
            script("readExcelDataWithin", "function readExcelDataWithin(source, maxRows) { return source == null ? [] : source; }", TRANSFER_RULE_SCOPE),
            script("readCsvDataWithin", "function readCsvDataWithin(source, maxRows) { return source == null ? [] : source; }", TRANSFER_RULE_SCOPE),
            script("rowText", "function rowText(row) { if (row == null) { return \"\"; }; text = \"\"; for (cell : row) { c = transferText(cell); if (c != \"\") { text = text + \" \" + c; }; }; return text; }", TRANSFER_RULE_SCOPE),
            script("rowContainsAllText", "function rowContainsAllText(row, keywords) { return containsAllText(rowText(row), keywords); }", TRANSFER_RULE_SCOPE),
            script("findHeaderRowIndexInRows", "function findHeaderRowIndexInRows(rows, keywords) { if (rows == null || keywords == null || keywords.length == 0) { return -1; }; for (i = 0; i < rows.length; i++) { if (rowContainsAllText(rows[i], keywords)) { return i; }; }; return -1; }", TRANSFER_RULE_SCOPE),
            script("findHeaderRowIndexWithin", "function findHeaderRowIndexWithin(source, maxRows, keyword1, keyword2) { return findHeaderRowIndexInRows(source, [keyword1, keyword2]); }", TRANSFER_RULE_SCOPE),
            script("hasHeaderKeywordsWithinFirstRows", "function hasHeaderKeywordsWithinFirstRows(source, maxRows, keyword1, keyword2) { return findHeaderRowIndexWithin(source, maxRows, keyword1, keyword2) >= 0; }", TRANSFER_RULE_SCOPE),
            script("hasHeaderKeywordsWithinFirstRowsByList", "function hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords) { return findHeaderRowIndexInRows(source, keywords) >= 0; }", TRANSFER_RULE_SCOPE),
            script("isValuationTable", "function isValuationTable(source, maxRows) { return hasHeaderKeywordsWithinFirstRows(source, maxRows, \"科目代码\", \"科目名称\"); }", TRANSFER_RULE_SCOPE),
            script("isValuationTableByKeywords", "function isValuationTableByKeywords(source, maxRows, keywords) { return hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords); }", TRANSFER_RULE_SCOPE),
            script("headerKeywordsFromMeta", "function headerKeywordsFromMeta(meta) { if (meta == null) { return [\"科目代码\", \"科目名称\"]; }; keywords = meta[\"headerKeywords\"]; if (keywords == null || keywords.length == 0) { return [\"科目代码\", \"科目名称\"]; }; return keywords; }", TRANSFER_RULE_SCOPE),
            script("isValuationTableByMeta", "function isValuationTableByMeta(source, meta) { return isValuationTableByKeywords(source, meta == null ? null : meta[\"scanLimit\"], headerKeywordsFromMeta(meta)); }", TRANSFER_RULE_SCOPE),
            script("productRuleValue", "function productRuleValue(rule, fieldName) { if (rule == null || fieldName == null) { return null; }; return rule[fieldName]; }", TRANSFER_RULE_SCOPE),
            script("productRuleMatches", "function productRuleMatches(fileName, rule) { keywords = productRuleValue(rule, \"matchKeywords\"); if (keywords != null && keywords.length > 0) { return containsAllText(fileName, keywords); }; return textMatches(fileName, productRuleValue(rule, \"matchRules\")); }", TRANSFER_RULE_SCOPE),
            script("firstProductMatchRule", "function firstProductMatchRule(fileName, productMatchRules) { if (fileName == null || productMatchRules == null) { return null; }; for (rule : productMatchRules) { if (productRuleMatches(fileName, rule)) { return rule; }; }; return null; }", TRANSFER_RULE_SCOPE),
            script("productMatchResult", "function productMatchResult(rule, fileName) { if (rule == null) { return null; }; snapshot = {\"ruleId\": productRuleValue(rule, \"id\"), \"pdCd\": productRuleValue(rule, \"pdCd\"), \"pdNm\": productRuleValue(rule, \"pdNm\"), \"orgCd\": productRuleValue(rule, \"orgCd\"), \"orgNm\": productRuleValue(rule, \"orgNm\"), \"pdType\": productRuleValue(rule, \"pdType\"), \"fileType\": productRuleValue(rule, \"fileType\"), \"fileTypeName\": productRuleValue(rule, \"fileTypeName\"), \"matchRules\": productRuleValue(rule, \"matchRules\"), \"jobName\": productRuleValue(rule, \"jobName\"), \"jobScene\": productRuleValue(rule, \"jobScene\")}; return {\"matched\": true, \"message\": \"产品匹配规则命中\", \"tagValue\": snapshot[\"ruleId\"], \"matchedField\": \"fileName\", \"matchedValue\": fileName, \"snapshot\": snapshot}; }", TRANSFER_RULE_SCOPE)
    ));

    private SystemQlexpressFunctionSeedScripts() {
    }

    public static List<QlexpressFunctionScript> scripts() {
        return SCRIPTS;
    }

    private static QlexpressFunctionScript script(String functionName, String scriptBody, String... sourceModules) {
        return new QlexpressFunctionScript(functionName, scriptBody, Arrays.asList(sourceModules));
    }
}
