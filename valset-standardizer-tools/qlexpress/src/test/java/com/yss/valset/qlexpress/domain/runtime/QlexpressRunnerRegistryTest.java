package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.QLOptions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QlexpressRunnerRegistryTest {

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
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(provider);
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
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(provider);
        ManagedQlexpressRunner runner = registry.createManagedRunner("transfer.rule");

        assertThat(runner.getRunner().execute("transferOnly()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("transfer");
        assertThat(runner.getRunner().execute("commonFn()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("common");
        assertThatThrownBy(() -> runner.getRunner().execute("parseOnly()", Collections.emptyMap(), QLOptions.DEFAULT_OPTIONS))
                .hasMessageContaining("parseOnly");
    }

    @Test
    void extractParseSeedFunctionsRunWithoutJavaFacade() {
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(SystemQlexpressFunctionSeedScripts::scripts, commonEnhancer());
        ManagedQlexpressRunner runner = registry.createManagedRunner(SystemQlexpressFunctionSeedScripts.EXTRACT_PARSE_SCOPE);
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("row", java.util.Arrays.asList("科目代码", "科目名称", "市值"));
        context.put("subjectRow", java.util.Arrays.asList("1001", "银行存款", "10"));
        context.put("shortCodeRow", java.util.Arrays.asList("101", "异常科目", "10"));
        context.put("customCodeRow", java.util.Arrays.asList("AB1001", "自定义科目", "10"));
        context.put("chineseCodeRow", java.util.Arrays.asList("AB科目1", "异常科目", "10"));
        context.put("metricRow", java.util.Arrays.asList("资产净值", "20"));
        context.put("requiredHeaders", java.util.Arrays.asList("科目代码", "科目名称"));
        context.put("footerKeywords", java.util.Arrays.asList("备注"));

        assertThat(runner.getRunner().execute("rowContainsAll(row, requiredHeaders)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isHeaderRow(row, requiredHeaders)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("classifyRowWithPattern(subjectRow, footerKeywords, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("SUBJECT");
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(shortCodeRow, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(false);
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(customCodeRow, '^AB[0-9]+$')", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(true);
        assertThat(runner.getRunner().execute("isSubjectRowWithPattern(chineseCodeRow, '^AB.*')", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo(false);
        assertThat(runner.getRunner().execute("classifyRowWithPattern(metricRow, footerKeywords, null)", context, QLOptions.DEFAULT_OPTIONS).getResult()).isEqualTo("METRIC_DATA");
        assertThat(runner.getRunner().execute("{\"subjectCode\": subjectRow[0], \"rawValues\": {\"value\": metricRow[1]}}", context, QLOptions.DEFAULT_OPTIONS).getResult())
                .isInstanceOf(Map.class);
    }

    @Test
    void extractParseSeedFunctionsDoNotUseRemovedBridgeOrUnsafeSyntax() {
        List<String> forbiddenFragments = java.util.Arrays.asList(
                "qlParseFns",
                "ParseRuleSupport",
                "row.get(",
                ".size()",
                ".contains("
        );

        for (QlexpressFunctionScript script : SystemQlexpressFunctionSeedScripts.scripts()) {
            if (!script.getSourceModules().contains(SystemQlexpressFunctionSeedScripts.EXTRACT_PARSE_SCOPE)) {
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
    void transferRuleSeedFunctionsRunWithoutJavaFacade() {
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(SystemQlexpressFunctionSeedScripts::scripts, commonEnhancer());
        ManagedQlexpressRunner runner = registry.createManagedRunner(SystemQlexpressFunctionSeedScripts.TRANSFER_RULE_SCOPE);
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
    void transferRuleSeedFunctionsDoNotUseJavaFacade() {
        List<String> forbiddenFragments = java.util.Arrays.asList(
                "qlTransferFns",
                "TransferRuleFunctions",
                "fn.",
                "String(",
                ".trim()",
                ".contains("
        );

        for (QlexpressFunctionScript script : SystemQlexpressFunctionSeedScripts.scripts()) {
            if (!script.getSourceModules().contains(SystemQlexpressFunctionSeedScripts.TRANSFER_RULE_SCOPE)) {
                continue;
            }
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(script.getScriptBody())
                        .as(script.getFunctionName() + " must not contain " + forbiddenFragment)
                        .doesNotContain(forbiddenFragment);
            }
        }
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
