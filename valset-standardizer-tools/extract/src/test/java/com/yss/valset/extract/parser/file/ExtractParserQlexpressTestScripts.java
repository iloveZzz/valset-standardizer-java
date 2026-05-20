package com.yss.valset.extract.parser.file;

import com.yss.valset.qlexpress.domain.runtime.QlexpressFunctionScript;

import java.util.List;

final class ExtractParserQlexpressTestScripts {

    private static final String COMMON_SCOPE = "common";
    private static final String EXTRACT_PARSE_SCOPE = "extract.parse";

    private ExtractParserQlexpressTestScripts() {
    }

    static List<QlexpressFunctionScript> scripts() {
        return java.util.Arrays.asList(
                script("regexMatches", "function regexMatches(source, regex) { return qlCommonFns[\"matchesRegex\"](source, regex); }", COMMON_SCOPE),
                script("rowContainsAll", "function rowContainsAll(row, keywords) { if (keywords == null || keywords.length == 0) { return false; }; return rowHitCount(row, keywords) >= keywords.length; }", EXTRACT_PARSE_SCOPE),
                script("rowContainsAny", "function rowContainsAny(row, keywords) { if (row == null || keywords == null) { return false; }; for (keyword : keywords) { if (keyword == null) { continue; }; kw = \"\" + keyword; if (kw == \"\") { continue; }; for (cell : row) { if (matchesKeyword(cell, kw)) { return true; }; }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("containsAny", "function containsAny(source, keywords) { if (source == null || keywords == null) { return false; }; text = \"\" + source; if (text == \"\") { return false; }; for (keyword : keywords) { if (matchesKeyword(text, keyword)) { return true; }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("containsAll", "function containsAll(source, keywords) { if (source == null || keywords == null || keywords.length == 0) { return false; }; text = \"\" + source; if (text == \"\") { return false; }; for (keyword : keywords) { if (!matchesKeyword(text, keyword)) { return false; }; }; return true; }", EXTRACT_PARSE_SCOPE),
                script("rowHitCount", "function rowHitCount(row, keywords) { if (row == null || keywords == null) { return 0; }; hitCount = 0; for (keyword : keywords) { if (keyword == null) { continue; }; kw = \"\" + keyword; if (kw == \"\") { continue; }; matched = false; for (cell : row) { if (matchesKeyword(cell, kw)) { matched = true; break; }; }; if (matched) { hitCount = hitCount + 1; }; }; return hitCount; }", EXTRACT_PARSE_SCOPE),
                script("matchesKeyword", "function matchesKeyword(source, keyword) { if (source == null || keyword == null) { return false; }; text = \"\" + source; kw = \"\" + keyword; if (text == \"\" || kw == \"\") { return false; }; return text == kw || kw in text || text in kw; }", EXTRACT_PARSE_SCOPE),
                script("isHeaderRow", "function isHeaderRow(row, requiredHeaders) { return rowContainsAll(row, requiredHeaders); }", EXTRACT_PARSE_SCOPE),
                script("isDataStartRow", "function isDataStartRow(row) { return isDataStartRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
                script("isDataStartRowWithPattern", "function isDataStartRowWithPattern(row, pattern) { return isSubjectRowWithPattern(row, pattern) || isMetricCandidateWithPattern(row, pattern); }", EXTRACT_PARSE_SCOPE),
                script("isSubjectRow", "function isSubjectRow(row) { return isSubjectRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
                script("isSubjectRowWithPattern", "function isSubjectRowWithPattern(row, pattern) { if (row == null) { return false; }; for (i = 0; i < row.length; i++) { if (isSubjectCodeText(row[i], pattern)) { return hasSubjectNameAfterCode(row, i); }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("isSubjectCodeText", "function isSubjectCodeText(value, pattern) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\") { return false; }; chineseTextPattern = \".*[一-龥].*\"; if (regexMatches(text, chineseTextPattern)) { return false; }; if (\"科\" in text || \"目\" in text || \"名\" in text || \"称\" in text || \"：\" in text || \":\" in text || \",\" in text || \"，\" in text) { return false; }; defaultSubjectCodePattern = \"^([0-9]{4}[A-Za-z0-9._ -]*|[A-Za-z][A-Za-z0-9._ -]*[0-9][A-Za-z0-9._ -]*)\\$\"; subjectCodePattern = pattern == null || pattern == \"\" ? defaultSubjectCodePattern : pattern; return regexMatches(text, subjectCodePattern); }", EXTRACT_PARSE_SCOPE),
                script("hasSubjectNameAfterCode", "function hasSubjectNameAfterCode(row, codeIndex) { if (row == null || codeIndex == null || codeIndex < 0) { return false; }; for (i = codeIndex + 1; i < row.length; i++) { text = textAt(row, i); if (text != \"\" && text != \"-\") { return true; }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("isSubjectDetailRowByColumn", "function isSubjectDetailRowByColumn(row, subjectCodeColumnIndex, pattern) { if (row == null || subjectCodeColumnIndex == null || subjectCodeColumnIndex < 0 || subjectCodeColumnIndex >= row.length) { return false; }; return isSubjectCodeText(textAt(row, subjectCodeColumnIndex), pattern) && hasSubjectNameAfterCode(row, subjectCodeColumnIndex); }", EXTRACT_PARSE_SCOPE),
                script("isMetricDetailRowByColumn", "function isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) { if (row == null || subjectCodeColumnIndex == null || subjectCodeColumnIndex < 0 || subjectCodeColumnIndex >= row.length || isFooterRow(row, null)) { return false; }; metricName = textAt(row, subjectCodeColumnIndex); if (!containsChineseText(metricName)) { return false; }; return rightNumericCount(row, subjectCodeColumnIndex) >= 1; }", EXTRACT_PARSE_SCOPE),
                script("isMetricDataRowByColumn", "function isMetricDataRowByColumn(row, subjectCodeColumnIndex, pattern) { return isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) && isRightNextNumeric(row, subjectCodeColumnIndex); }", EXTRACT_PARSE_SCOPE),
                script("isMetricRowByColumn", "function isMetricRowByColumn(row, subjectCodeColumnIndex, pattern) { return isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) && !isRightNextNumeric(row, subjectCodeColumnIndex); }", EXTRACT_PARSE_SCOPE),
                script("isValuationDataRowByColumn", "function isValuationDataRowByColumn(row, subjectCodeColumnIndex, pattern) { return isSubjectDetailRowByColumn(row, subjectCodeColumnIndex, pattern) || isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern); }", EXTRACT_PARSE_SCOPE),
                script("isMetricCandidate", "function isMetricCandidate(row) { return isMetricCandidateWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
                script("isMetricCandidateWithPattern", "function isMetricCandidateWithPattern(row, pattern) { return isMetricDataRowWithPattern(row, pattern) || isMetricRowWithPattern(row, pattern); }", EXTRACT_PARSE_SCOPE),
                script("isMetricDataRow", "function isMetricDataRow(row) { return isMetricDataRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
                script("isMetricDataRowWithPattern", "function isMetricDataRowWithPattern(row, pattern) { if (row == null || row.length < 2) { return false; }; firstCell = textAt(row, 0); if (firstCell == \"\" || isSubjectCodeText(firstCell, pattern)) { return false; }; if (textAt(row, 1) == \"\") { return false; }; for (i = 2; i < row.length; i++) { if (textAt(row, i) != \"\") { return false; }; }; return true; }", EXTRACT_PARSE_SCOPE),
                script("isMetricRow", "function isMetricRow(row) { return isMetricRowWithPattern(row, null); }", EXTRACT_PARSE_SCOPE),
                script("isMetricRowWithPattern", "function isMetricRowWithPattern(row, pattern) { if (row == null || row.length < 2) { return false; }; firstCell = textAt(row, 0); if (firstCell == \"\" || isSubjectCodeText(firstCell, pattern)) { return false; }; filledCount = 0; for (i = 1; i < row.length; i++) { if (textAt(row, i) != \"\") { filledCount = filledCount + 1; }; }; return filledCount >= 2; }", EXTRACT_PARSE_SCOPE),
                script("isFooterRow", "function isFooterRow(row, footerKeywords) { keywords = footerKeywords; if (keywords == null || keywords.length == 0) { keywords = [\"制表\", \"复核\", \"经办\"]; }; return rowContainsAny(row, keywords); }", EXTRACT_PARSE_SCOPE),
                script("classifyRow", "function classifyRow(row, footerKeywords) { return classifyRowWithPattern(row, footerKeywords, null); }", EXTRACT_PARSE_SCOPE),
                script("classifyRowWithPattern", "function classifyRowWithPattern(row, footerKeywords, pattern) { if (isSubjectRowWithPattern(row, pattern)) { return \"SUBJECT\"; }; if (isMetricDataRowWithPattern(row, pattern)) { return \"METRIC_DATA\"; }; if (isMetricRowWithPattern(row, pattern)) { return \"METRIC_ROW\"; }; if (isFooterRow(row, footerKeywords)) { return \"FOOTER\"; }; return \"IGNORE\"; }", EXTRACT_PARSE_SCOPE),
                script("firstMeaningfulTextContainsAny", "function firstMeaningfulTextContainsAny(row, keywords) { return containsAny(firstMeaningfulText(row), keywords); }", EXTRACT_PARSE_SCOPE),
                script("firstMeaningfulTextContainsAll", "function firstMeaningfulTextContainsAll(row, keywords) { return containsAll(firstMeaningfulText(row), keywords); }", EXTRACT_PARSE_SCOPE),
                script("hasAtLeastNonBlank", "function hasAtLeastNonBlank(row, minCount) { return minCount != null && minCount > 0 && rowNonBlankCount(row) >= minCount; }", EXTRACT_PARSE_SCOPE),
                script("textAt", "function textAt(row, index) { if (row == null || index == null || index < 0 || index >= row.length || row[index] == null) { return \"\"; }; return \"\" + row[index]; }", EXTRACT_PARSE_SCOPE),
                script("valueAt", "function valueAt(row, index) { if (row == null || index == null || index < 0 || index >= row.length) { return null; }; return row[index]; }", EXTRACT_PARSE_SCOPE),
                script("rowNonBlankCount", "function rowNonBlankCount(row) { if (row == null) { return 0; }; count = 0; for (cell : row) { text = cell == null ? \"\" : \"\" + cell; if (text != \"\" && text != \"-\") { count = count + 1; }; }; return count; }", EXTRACT_PARSE_SCOPE),
                script("firstMeaningfulText", "function firstMeaningfulText(row) { if (row == null) { return \"\"; }; for (cell : row) { text = cell == null ? \"\" : \"\" + cell; if (text != \"\" && text != \"-\") { return text; }; }; return \"\"; }", EXTRACT_PARSE_SCOPE),
                script("containsChineseText", "function containsChineseText(value) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\" || text == \"0\") { return false; }; chineseTextPattern = \".*[一-龥].*\"; return regexMatches(text, chineseTextPattern); }", EXTRACT_PARSE_SCOPE),
                script("isNumericText", "function isNumericText(value) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\") { return false; }; numberWithThousandsPattern = \"^-?[0-9]+(,[0-9]{3})*([.][0-9]+)?%?\\$\"; numberPattern = \"^-?[0-9]+([.][0-9]+)?%?\\$\"; return regexMatches(text, numberWithThousandsPattern) || regexMatches(text, numberPattern); }", EXTRACT_PARSE_SCOPE),
                script("rightNumericCount", "function rightNumericCount(row, startIndex) { if (row == null || startIndex == null || startIndex < 0 || startIndex >= row.length) { return 0; }; count = 0; for (i = startIndex + 1; i < row.length; i++) { if (isNumericText(textAt(row, i))) { count = count + 1; }; }; return count; }", EXTRACT_PARSE_SCOPE),
                script("isRightNextNumeric", "function isRightNextNumeric(row, startIndex) { if (row == null || startIndex == null || startIndex < 0 || startIndex + 1 >= row.length) { return false; }; return isNumericText(textAt(row, startIndex + 1)); }", EXTRACT_PARSE_SCOPE),
                script("mapOf", "function mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10) { m = {:}; if (k1 != null) { m[k1] = v1; }; if (k2 != null) { m[k2] = v2; }; if (k3 != null) { m[k3] = v3; }; if (k4 != null) { m[k4] = v4; }; if (k5 != null) { m[k5] = v5; }; if (k6 != null) { m[k6] = v6; }; if (k7 != null) { m[k7] = v7; }; if (k8 != null) { m[k8] = v8; }; if (k9 != null) { m[k9] = v9; }; if (k10 != null) { m[k10] = v10; }; return m; }", EXTRACT_PARSE_SCOPE)
        );
    }

    private static QlexpressFunctionScript script(String functionName, String scriptBody, String sourceModule) {
        return new QlexpressFunctionScript(functionName, scriptBody, java.util.Collections.singletonList(sourceModule));
    }
}
