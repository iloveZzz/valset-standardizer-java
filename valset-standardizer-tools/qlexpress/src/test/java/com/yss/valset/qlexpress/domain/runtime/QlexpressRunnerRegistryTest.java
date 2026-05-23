package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QlexpressRunnerRegistryTest {

    private static final String COMMON_SCOPE = "common";
    private static final String EXTRACT_PARSE_SCOPE = "extract.parse";
    private static final String TRANSFER_RULE_SCOPE = "transfer.rule";

    @Test
    void validateScriptRejectsMismatchedFunctionName() {
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(() -> Collections.emptyList());

        assertThatThrownBy(() -> registry.validateScript("expectedName", "function actualName() { return true; }"))
                .hasMessageContaining("expectedName");
    }

    @Test
    void managedRunnerRefreshRebuildsEnabledFunctionSet() {
        MutableProvider provider = new MutableProvider();
        provider.scripts = Collections.singletonList(new QlexpressFunctionScript("foo", "function foo() { return 1; }"));
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(provider, QlexpressExecutionContextEnhancer.empty(), 2);
        ManagedQlexpressRunner runner = registry.createManagedRunner();

        assertThat(runner.getRunner().execute("foo()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(1);

        provider.scripts = Collections.singletonList(new QlexpressFunctionScript("bar", "function bar() { return 2; }"));
        registry.refreshAll();

        assertThat(runner.getRunner().execute("bar()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(2);
        assertThatThrownBy(() -> runner.getRunner().execute("foo()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS))
                .hasMessageContaining("foo");
    }

    @Test
    void managedRunnerLoadsEnabledFunctionsByScope() {
        MutableProvider provider = new MutableProvider();
        provider.scripts = java.util.Arrays.asList(
                new QlexpressFunctionScript("parseOnly", "function parseOnly() { return 'parse'; }", Collections.singletonList("extract.parse")),
                new QlexpressFunctionScript("transferOnly", "function transferOnly() { return 'transfer'; }", Collections.singletonList("transfer.rule")),
                new QlexpressFunctionScript("commonFn", "function commonFn() { return 'common'; }", Collections.singletonList("common"))
        );
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(provider, QlexpressExecutionContextEnhancer.empty(), 2);
        ManagedQlexpressRunner runner = registry.createManagedRunner("transfer.rule");

        assertThat(runner.getRunner().execute("transferOnly()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("transfer");
        assertThat(runner.getRunner().execute("commonFn()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("common");
        assertThatThrownBy(() -> runner.getRunner().execute("parseOnly()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS))
                .hasMessageContaining("parseOnly");
    }

    @Test
    void managedRunnerNormalizesLeadingLogicalOperatorsInPersistedScripts() {
        MutableProvider provider = new MutableProvider();
        provider.scripts = Collections.singletonList(new QlexpressFunctionScript(
                "compound",
                "function compound(left, right, fallback) {\n"
                        + "  return left\n"
                        + "    && right\n"
                        + "    || fallback;\n"
                        + "}",
                Collections.singletonList(EXTRACT_PARSE_SCOPE)
        ));
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(provider, QlexpressExecutionContextEnhancer.empty(), 2);
        ManagedQlexpressRunner runner = registry.createManagedRunner(EXTRACT_PARSE_SCOPE);

        Map<String, Object> context = new java.util.HashMap<>();
        context.put("left", true);
        context.put("right", false);
        context.put("fallback", true);

        assertThat(runner.getRunner().execute("compound(left, right, fallback)", context, QLOptions.DEFAULT_OPTIONS).getResult())
                .isEqualTo(true);
    }

    @Test
    void extractParseConfiguredFunctionsRunWithoutJavaFacade() {
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(QlexpressRunnerRegistryTest::extractParseScripts, commonEnhancer(), 2);
        ManagedQlexpressRunner runner = registry.createManagedRunner(EXTRACT_PARSE_SCOPE);
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("row", java.util.Arrays.asList("科目代码", "科目名称", "市值"));
        context.put("subjectRow", java.util.Arrays.asList("1001", "银行存款", "10"));
        context.put("shortCodeRow", java.util.Arrays.asList("101", "异常科目", "10"));
        context.put("customCodeRow", java.util.Arrays.asList("AB1001", "自定义科目", "10"));
        context.put("spacedCodeRow", java.util.Arrays.asList("1002.04.01.FBTYCK24032001 CW", "同业存款", "10"));
        context.put("chineseCodeRow", java.util.Arrays.asList("AB科目1", "异常科目", "10"));
        context.put("metricRow", java.util.Arrays.asList("资产净值", "20"));
        context.put("metricDetailRow", java.util.Arrays.asList("资产净值", "20"));
        context.put("multiMetricDetailRow", java.util.Arrays.asList("其他指标", "-", "40"));
        context.put("invalidMetricRow", java.util.Arrays.asList("0", "20"));
        context.put("requiredHeaders", java.util.Arrays.asList("科目代码", "科目名称"));
        context.put("footerKeywords", java.util.Arrays.asList("备注"));

        assertThat(runner.getRunner().execute("rowContainsAll(row, requiredHeaders)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isHeaderRow(row, requiredHeaders)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("classifyRowWithPattern(subjectRow, footerKeywords, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("SUBJECT");
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(shortCodeRow, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(false);
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(customCodeRow, '^AB[0-9]+$')", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(spacedCodeRow, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(chineseCodeRow, '^AB.*')", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(false);
        assertThat(runner.getRunner().execute("classifyRowWithPattern(metricRow, footerKeywords, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("METRIC_DATA");
        assertThat(runner.getRunner().execute("isMetricDetailRowByColumn(metricDetailRow, 0, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isMetricDataRowByColumn(metricDetailRow, 0, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isMetricRowByColumn(multiMetricDetailRow, 0, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isMetricDetailRowByColumn(invalidMetricRow, 0, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(false);
        assertThat(runner.getRunner().execute("{\"subjectCode\": subjectRow[0], \"rawValues\": {\"value\": metricRow[1]}}", context, QLOptions.DEFAULT_OPTIONS).getResult())
                .isInstanceOf(Map.class);
    }

    @Test
    void extractParseConfiguredFunctionsDoNotUseRemovedBridgeOrUnsafeSyntax() {
        List<String> forbiddenFragments = java.util.Arrays.asList(
                "qlParseFns",
                "ParseRuleSupport",
                "row.get(",
                ".size()",
                ".contains("
        );

        for (QlexpressFunctionScript script : extractParseScripts()) {
            if (!script.getSourceModules().contains(EXTRACT_PARSE_SCOPE)) {
                continue;
            }
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(script.getScriptBody())
                        .as(script.getFunctionName() + " must not contain " + forbiddenFragment)
                        .doesNotContain(forbiddenFragment);
            }
        }
    }

    @Test
    void transferRuleConfiguredFunctionsRunWithoutJavaFacade() {
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(QlexpressRunnerRegistryTest::transferRuleScripts, commonEnhancer(), 2);
        ManagedQlexpressRunner runner = registry.createManagedRunner(TRANSFER_RULE_SCOPE);
        Map<String, Object> context = new java.util.HashMap<>();
        Map<String, Object> rule = new java.util.HashMap<>();
        rule.put("id", "26169");
        rule.put("pdCd", "NYADTCZQSM2419");
        rule.put("pdNm", "农银理财产品");
        rule.put("matchKeywords", java.util.Arrays.asList("农银理财产品", "估值表"));
        context.put("fileName", "TA_农银理财产品_估值表.xlsx");
        context.put("productMatchRules", java.util.Arrays.asList(rule));
        context.put("previewRows", java.util.Arrays.asList(
                java.util.Arrays.asList("标题"),
                java.util.Arrays.asList("科目代码", "科目名称", "市值")
        ));
        context.put("tagMeta", java.util.Collections.singletonMap("headerKeywords", java.util.Arrays.asList("科目代码", "科目名称")));

        assertThat(runner.getRunner().execute("containsAnyText(fileName, [\"估值表\"])", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isExcelFile(fileName)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isValuationTableByMeta(previewRows, tagMeta)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("productMatchResult(firstProductMatchRule(fileName, productMatchRules), fileName)", context, QLOptions.DEFAULT_OPTIONS).getResult())
                .isInstanceOf(Map.class);
    }

    @Test
    void transferRuleConfiguredFunctionsDoNotUseJavaFacade() {
        List<String> forbiddenFragments = java.util.Arrays.asList(
                "qlTransferFns",
                "TransferRuleFunctions",
                "fn.",
                "String(",
                ".trim()",
                ".contains("
        );

        for (QlexpressFunctionScript script : transferRuleScripts()) {
            if (!script.getSourceModules().contains(TRANSFER_RULE_SCOPE)) {
                continue;
            }
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(script.getScriptBody())
                        .as(script.getFunctionName() + " must not contain " + forbiddenFragment)
                        .doesNotContain(forbiddenFragment);
            }
        }
    }

    @Test
    void managedRunnerSupportsBoundedConcurrentExecution() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);
        ManagedQlexpressRunner runner = new ManagedQlexpressRunner(
                () -> null,
                2,
                () -> (expression, context, options) -> {
                    int current = active.incrementAndGet();
                    maxActive.accumulateAndGet(current, Math::max);
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(exception);
                    } finally {
                        active.decrementAndGet();
                    }
                    Object value = context == null ? null : context.get("value");
                    return value instanceof Number ? ((Number) value).intValue() + 1 : 1;
                }
        );

        int taskCount = 6;
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(taskCount);
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);

        for (int i = 0; i < taskCount; i++) {
            final int value = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    Object result = runner.executeResult("ignored", Collections.singletonMap("value", value), QLOptions.DEFAULT_OPTIONS);
                    assertThat(result).isEqualTo(value + 1);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        release.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
        assertThat(maxActive.get()).isLessThanOrEqualTo(2);
    }

    private static List<QlexpressFunctionScript> extractParseScripts() {
        return java.util.Arrays.asList(
                script("regexMatches", "function regexMatches(source, regex) { return qlCommonFns[\"matchesRegex\"](source, regex); }", COMMON_SCOPE),
                script("rowContainsAll", "function rowContainsAll(row, keywords) { if (keywords == null || keywords.length == 0) { return false; }; return rowHitCount(row, keywords) >= keywords.length; }", EXTRACT_PARSE_SCOPE),
                script("rowHitCount", "function rowHitCount(row, keywords) { if (row == null || keywords == null) { return 0; }; hitCount = 0; for (keyword : keywords) { if (keyword == null) { continue; }; kw = \"\" + keyword; if (kw == \"\") { continue; }; matched = false; for (cell : row) { if (matchesKeyword(cell, kw)) { matched = true; break; }; }; if (matched) { hitCount = hitCount + 1; }; }; return hitCount; }", EXTRACT_PARSE_SCOPE),
                script("rowContainsAny", "function rowContainsAny(row, keywords) { if (row == null || keywords == null) { return false; }; for (keyword : keywords) { if (keyword == null) { continue; }; kw = \"\" + keyword; if (kw == \"\") { continue; }; for (cell : row) { if (matchesKeyword(cell, kw)) { return true; }; }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("matchesKeyword", "function matchesKeyword(source, keyword) { if (source == null || keyword == null) { return false; }; text = \"\" + source; kw = \"\" + keyword; if (text == \"\" || kw == \"\") { return false; }; return text == kw || kw in text || text in kw; }", EXTRACT_PARSE_SCOPE),
                script("isHeaderRow", "function isHeaderRow(row, requiredHeaders) { return rowContainsAll(row, requiredHeaders); }", EXTRACT_PARSE_SCOPE),
                script("classifyRowWithPattern", "function classifyRowWithPattern(row, footerKeywords, pattern) { if (isSubjectRowWithPattern(row, pattern)) { return \"SUBJECT\"; }; if (isMetricDataRowWithPattern(row, pattern)) { return \"METRIC_DATA\"; }; if (isMetricRowWithPattern(row, pattern)) { return \"METRIC_ROW\"; }; if (isFooterRow(row, footerKeywords)) { return \"FOOTER\"; }; return \"IGNORE\"; }", EXTRACT_PARSE_SCOPE),
                script("isSubjectRowWithPattern", "function isSubjectRowWithPattern(row, pattern) { if (row == null) { return false; }; for (i = 0; i < row.length; i++) { if (isSubjectCodeText(row[i], pattern)) { return hasSubjectNameAfterCode(row, i); }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("isSubjectCodeText", "function isSubjectCodeText(value, pattern) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\") { return false; }; chineseTextPattern = \".*[一-龥].*\"; if (regexMatches(text, chineseTextPattern)) { return false; }; if (\"科\" in text || \"目\" in text || \"名\" in text || \"称\" in text || \"：\" in text || \":\" in text || \",\" in text || \"，\" in text) { return false; }; defaultSubjectCodePattern = \"^([0-9]{4}[A-Za-z0-9._ -]*|[A-Za-z][A-Za-z0-9._ -]*[0-9][A-Za-z0-9._ -]*)\\$\"; subjectCodePattern = pattern == null || pattern == \"\" ? defaultSubjectCodePattern : pattern; return regexMatches(text, subjectCodePattern); }", EXTRACT_PARSE_SCOPE),
                script("hasSubjectNameAfterCode", "function hasSubjectNameAfterCode(row, codeIndex) { if (row == null || codeIndex == null || codeIndex < 0) { return false; }; for (i = codeIndex + 1; i < row.length; i++) { text = textAt(row, i); if (text != \"\" && text != \"-\") { return true; }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("isSubjectDetailRowByColumn", "function isSubjectDetailRowByColumn(row, subjectCodeColumnIndex, pattern) { if (row == null || subjectCodeColumnIndex == null || subjectCodeColumnIndex < 0 || subjectCodeColumnIndex >= row.length) { return false; }; return isSubjectCodeText(textAt(row, subjectCodeColumnIndex), pattern) && hasSubjectNameAfterCode(row, subjectCodeColumnIndex); }", EXTRACT_PARSE_SCOPE),
                script("isMetricDetailRowByColumn", "function isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) { if (row == null || subjectCodeColumnIndex == null || subjectCodeColumnIndex < 0 || subjectCodeColumnIndex >= row.length || isFooterRow(row, null)) { return false; }; metricName = textAt(row, subjectCodeColumnIndex); if (!containsChineseText(metricName)) { return false; }; return rightNumericCount(row, subjectCodeColumnIndex) >= 1; }", EXTRACT_PARSE_SCOPE),
                script("isMetricDataRowByColumn", "function isMetricDataRowByColumn(row, subjectCodeColumnIndex, pattern) { return isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) && isRightNextNumeric(row, subjectCodeColumnIndex); }", EXTRACT_PARSE_SCOPE),
                script("isMetricRowByColumn", "function isMetricRowByColumn(row, subjectCodeColumnIndex, pattern) { return isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) && !isRightNextNumeric(row, subjectCodeColumnIndex); }", EXTRACT_PARSE_SCOPE),
                script("isMetricDataRowWithPattern", "function isMetricDataRowWithPattern(row, pattern) { if (row == null || row.length < 2) { return false; }; firstCell = textAt(row, 0); if (firstCell == \"\" || isSubjectCodeText(firstCell, pattern)) { return false; }; if (textAt(row, 1) == \"\") { return false; }; for (i = 2; i < row.length; i++) { if (textAt(row, i) != \"\") { return false; }; }; return true; }", EXTRACT_PARSE_SCOPE),
                script("isMetricRowWithPattern", "function isMetricRowWithPattern(row, pattern) { if (row == null || row.length < 2) { return false; }; firstCell = textAt(row, 0); if (firstCell == \"\" || isSubjectCodeText(firstCell, pattern)) { return false; }; filledCount = 0; for (i = 1; i < row.length; i++) { if (textAt(row, i) != \"\") { filledCount = filledCount + 1; }; }; return filledCount >= 2; }", EXTRACT_PARSE_SCOPE),
                script("isFooterRow", "function isFooterRow(row, footerKeywords) { keywords = footerKeywords; if (keywords == null || keywords.length == 0) { keywords = [\"制表\", \"复核\", \"经办\"]; }; return rowContainsAny(row, keywords); }", EXTRACT_PARSE_SCOPE),
                script("containsAny", "function containsAny(source, keywords) { if (source == null || keywords == null) { return false; }; text = \"\" + source; if (text == \"\") { return false; }; for (keyword : keywords) { if (matchesKeyword(text, keyword)) { return true; }; }; return false; }", EXTRACT_PARSE_SCOPE),
                script("textAt", "function textAt(row, index) { if (row == null || index == null || index < 0 || index >= row.length || row[index] == null) { return \"\"; }; return \"\" + row[index]; }", EXTRACT_PARSE_SCOPE),
                script("firstMeaningfulText", "function firstMeaningfulText(row) { if (row == null) { return \"\"; }; for (cell : row) { text = cell == null ? \"\" : \"\" + cell; if (text != \"\" && text != \"-\") { return text; }; }; return \"\"; }", EXTRACT_PARSE_SCOPE),
                script("containsChineseText", "function containsChineseText(value) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\" || text == \"0\") { return false; }; chineseTextPattern = \".*[一-龥].*\"; return regexMatches(text, chineseTextPattern); }", EXTRACT_PARSE_SCOPE),
                script("isNumericText", "function isNumericText(value) { if (value == null) { return false; }; text = \"\" + value; if (text == \"\" || text == \"-\") { return false; }; numberWithThousandsPattern = \"^-?[0-9]+(,[0-9]{3})*([.][0-9]+)?%?\\$\"; numberPattern = \"^-?[0-9]+([.][0-9]+)?%?\\$\"; return regexMatches(text, numberWithThousandsPattern) || regexMatches(text, numberPattern); }", EXTRACT_PARSE_SCOPE),
                script("rightNumericCount", "function rightNumericCount(row, startIndex) { if (row == null || startIndex == null || startIndex < 0 || startIndex >= row.length) { return 0; }; count = 0; for (i = startIndex + 1; i < row.length; i++) { if (isNumericText(textAt(row, i))) { count = count + 1; }; }; return count; }", EXTRACT_PARSE_SCOPE),
                script("isRightNextNumeric", "function isRightNextNumeric(row, startIndex) { if (row == null || startIndex == null || startIndex < 0 || startIndex + 1 >= row.length) { return false; }; return isNumericText(textAt(row, startIndex + 1)); }", EXTRACT_PARSE_SCOPE)
        );
    }

    private static List<QlexpressFunctionScript> transferRuleScripts() {
        return java.util.Arrays.asList(
                script("transferText", "function transferText(value) { return value == null ? \"\" : \"\" + value; }", TRANSFER_RULE_SCOPE),
                script("textMatches", "function textMatches(source, keyword) { text = transferText(source); kw = transferText(keyword); if (text == \"\" || kw == \"\") { return false; }; return text == kw || kw in text || text in kw || text like kw; }", TRANSFER_RULE_SCOPE),
                script("containsAnyText", "function containsAnyText(source, keywords) { if (source == null || keywords == null) { return false; }; for (keyword : keywords) { if (textMatches(source, keyword)) { return true; }; }; return false; }", TRANSFER_RULE_SCOPE),
                script("containsAllText", "function containsAllText(source, keywords) { if (source == null || keywords == null || keywords.length == 0) { return false; }; for (keyword : keywords) { if (!textMatches(source, keyword)) { return false; }; }; return true; }", TRANSFER_RULE_SCOPE),
                script("isExcel", "function isExcel(fileName) { text = transferText(fileName); return text like \"%.xlsx\" || text like \"%.xls\" || text like \"%.XLSX\" || text like \"%.XLS\"; }", TRANSFER_RULE_SCOPE),
                script("isExcelFile", "function isExcelFile(fileName) { return isExcel(fileName); }", TRANSFER_RULE_SCOPE),
                script("rowText", "function rowText(row) { if (row == null) { return \"\"; }; text = \"\"; for (cell : row) { c = transferText(cell); if (c != \"\") { text = text + \" \" + c; }; }; return text; }", TRANSFER_RULE_SCOPE),
                script("rowContainsAllText", "function rowContainsAllText(row, keywords) { return containsAllText(rowText(row), keywords); }", TRANSFER_RULE_SCOPE),
                script("findHeaderRowIndexInRows", "function findHeaderRowIndexInRows(rows, keywords) { if (rows == null || keywords == null || keywords.length == 0) { return -1; }; for (i = 0; i < rows.length; i++) { if (rowContainsAllText(rows[i], keywords)) { return i; }; }; return -1; }", TRANSFER_RULE_SCOPE),
                script("hasHeaderKeywordsWithinFirstRowsByList", "function hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords) { return findHeaderRowIndexInRows(source, keywords) >= 0; }", TRANSFER_RULE_SCOPE),
                script("isValuationTableByKeywords", "function isValuationTableByKeywords(source, maxRows, keywords) { return hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords); }", TRANSFER_RULE_SCOPE),
                script("headerKeywordsFromMeta", "function headerKeywordsFromMeta(meta) { if (meta == null) { return [\"科目代码\", \"科目名称\"]; }; keywords = meta[\"headerKeywords\"]; if (keywords == null || keywords.length == 0) { return [\"科目代码\", \"科目名称\"]; }; return keywords; }", TRANSFER_RULE_SCOPE),
                script("isValuationTableByMeta", "function isValuationTableByMeta(source, meta) { return isValuationTableByKeywords(source, meta == null ? null : meta[\"scanLimit\"], headerKeywordsFromMeta(meta)); }", TRANSFER_RULE_SCOPE),
                script("productRuleValue", "function productRuleValue(rule, fieldName) { if (rule == null || fieldName == null) { return null; }; return rule[fieldName]; }", TRANSFER_RULE_SCOPE),
                script("productRuleMatches", "function productRuleMatches(fileName, rule) { keywords = productRuleValue(rule, \"matchKeywords\"); if (keywords != null && keywords.length > 0) { return containsAllText(fileName, keywords); }; return textMatches(fileName, productRuleValue(rule, \"matchRules\")); }", TRANSFER_RULE_SCOPE),
                script("firstProductMatchRule", "function firstProductMatchRule(fileName, productMatchRules) { if (fileName == null || productMatchRules == null) { return null; }; for (rule : productMatchRules) { if (productRuleMatches(fileName, rule)) { return rule; }; }; return null; }", TRANSFER_RULE_SCOPE),
                script("productMatchResult", "function productMatchResult(rule, fileName) { if (rule == null) { return null; }; snapshot = {\"ruleId\": productRuleValue(rule, \"id\"), \"pdCd\": productRuleValue(rule, \"pdCd\"), \"pdNm\": productRuleValue(rule, \"pdNm\"), \"orgCd\": productRuleValue(rule, \"orgCd\"), \"orgNm\": productRuleValue(rule, \"orgNm\"), \"pdType\": productRuleValue(rule, \"pdType\"), \"fileType\": productRuleValue(rule, \"fileType\"), \"fileTypeName\": productRuleValue(rule, \"fileTypeName\"), \"matchRules\": productRuleValue(rule, \"matchRules\"), \"jobName\": productRuleValue(rule, \"jobName\"), \"jobScene\": productRuleValue(rule, \"jobScene\")}; return {\"matched\": true, \"message\": \"产品匹配规则命中\", \"tagValue\": snapshot[\"ruleId\"], \"matchedField\": \"fileName\", \"matchedValue\": fileName, \"snapshot\": snapshot}; }", TRANSFER_RULE_SCOPE)
        );
    }

    private static QlexpressFunctionScript script(String functionName, String scriptBody, String sourceModule) {
        return new QlexpressFunctionScript(functionName, scriptBody, Collections.singletonList(sourceModule));
    }

    private static class MutableProvider implements QlexpressFunctionScriptProvider {
        private List<QlexpressFunctionScript> scripts = Collections.emptyList();

        @Override
        public List<QlexpressFunctionScript> listEnabledScripts() {
            return scripts;
        }
    }

    private static QlexpressExecutionContextEnhancer commonEnhancer() {
        return new QlexpressExecutionContextEnhancer(
                java.util.Collections.singletonList(new QlexpressCommonContextContributor(new QlexpressCommonFunctionFacade()))
        );
    }
}
