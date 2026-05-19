package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.api.BatchAddFunctionResult;
import com.alibaba.qlexpress4.runtime.context.MapExpressContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * QLExpress Runner 工厂与刷新注册中心。
 */
@Slf4j
@Component
public class QlexpressRunnerRegistry {

    private final ObjectProvider<QlexpressFunctionScriptProvider> scriptProvider;
    private final QlexpressFunctionScriptProvider directScriptProvider;
    private final QlexpressExecutionContextEnhancer contextEnhancer;
    private final List<QlexpressRunnerHolder> holders = new CopyOnWriteArrayList<>();

    @Autowired
    public QlexpressRunnerRegistry(ObjectProvider<QlexpressFunctionScriptProvider> scriptProvider,
                                   QlexpressExecutionContextEnhancer contextEnhancer) {
        this.scriptProvider = scriptProvider;
        this.directScriptProvider = null;
        this.contextEnhancer = contextEnhancer == null ? QlexpressExecutionContextEnhancer.empty() : contextEnhancer;
    }

    public QlexpressRunnerRegistry(QlexpressFunctionScriptProvider scriptProvider) {
        this(scriptProvider, QlexpressExecutionContextEnhancer.empty());
    }

    public QlexpressRunnerRegistry(QlexpressFunctionScriptProvider scriptProvider, QlexpressExecutionContextEnhancer contextEnhancer) {
        this.scriptProvider = null;
        this.directScriptProvider = scriptProvider == null ? Collections::emptyList : scriptProvider;
        this.contextEnhancer = contextEnhancer == null ? QlexpressExecutionContextEnhancer.empty() : contextEnhancer;
    }

    public ManagedQlexpressRunner createManagedRunner() {
        return createManagedRunner(null);
    }

    public ManagedQlexpressRunner createManagedRunner(String runnerScope) {
        ManagedQlexpressRunner holder = new ManagedQlexpressRunner(() -> createRunner(runnerScope, listEnabledScripts(runnerScope)));
        holders.add(holder);
        return holder;
    }

    public Express4Runner createSandboxRunner(List<QlexpressFunctionScript> extraScripts) {
        return createSandboxRunner(null, extraScripts);
    }

    public Express4Runner createSandboxRunner(String runnerScope, List<QlexpressFunctionScript> extraScripts) {
        List<QlexpressFunctionScript> safeExtraScripts = extraScripts == null ? Collections.emptyList() : extraScripts;
        Set<String> extraFunctionNames = safeExtraScripts.stream()
                .filter(script -> script != null && !isBlank(script.getFunctionName()))
                .map(QlexpressFunctionScript::getFunctionName)
                .collect(Collectors.toSet());
        List<QlexpressFunctionScript> scripts = listEnabledScripts(runnerScope).stream()
                .filter(script -> !extraFunctionNames.contains(script.getFunctionName()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (extraScripts != null && !extraScripts.isEmpty()) {
            scripts.addAll(extraScripts);
        }
        return createRunner(runnerScope, scripts);
    }

    public void validateScript(String expectedFunctionName, String scriptBody) {
        validateScripts(Collections.singletonList(new QlexpressFunctionScript(expectedFunctionName, scriptBody)));
    }

    public void validateScripts(List<QlexpressFunctionScript> scripts) {
        createRunner(null, scripts == null ? Collections.emptyList() : scripts);
    }

    public Set<String> getOutFunctions(String expression) {
        return createSandboxRunner(Collections.emptyList()).getOutFunctions(expression);
    }

    public Set<String> getOutVarNames(String expression) {
        return createSandboxRunner(Collections.emptyList()).getOutVarNames(expression);
    }

    public void refreshAll() {
        for (QlexpressRunnerHolder holder : holders) {
            holder.refresh();
        }
        log.info("已刷新 QLExpress 托管 Runner，count={}", holders.size());
    }

    private Express4Runner createRunner(String runnerScope, List<QlexpressFunctionScript> scripts) {
        Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
        if (scripts == null || scripts.isEmpty()) {
            return runner;
        }
        for (QlexpressFunctionScript script : scripts) {
            registerOne(runner, runnerScope, script);
        }
        return runner;
    }

    private List<QlexpressFunctionScript> listEnabledScripts() {
        return listEnabledScripts(null);
    }

    private List<QlexpressFunctionScript> listEnabledScripts(String runnerScope) {
        QlexpressFunctionScriptProvider provider = directScriptProvider;
        if (provider == null && scriptProvider != null) {
            provider = scriptProvider.getIfAvailable();
        }
        if (provider == null) {
            return Collections.emptyList();
        }
        List<QlexpressFunctionScript> scripts = provider.listEnabledScripts();
        if (scripts == null || scripts.isEmpty()) {
            return Collections.emptyList();
        }
        return scripts.stream()
                .filter(script -> matchesScope(script, runnerScope))
                .collect(Collectors.toList());
    }

    private boolean matchesScope(QlexpressFunctionScript script, String runnerScope) {
        if (script == null || isBlank(runnerScope)) {
            return true;
        }
        List<String> modules = script.getSourceModules();
        if (modules == null || modules.isEmpty()) {
            return true;
        }
        for (String module : modules) {
            if ("common".equalsIgnoreCase(module) || runnerScope.equalsIgnoreCase(module)) {
                return true;
            }
        }
        return false;
    }

    private void registerOne(Express4Runner runner, String runnerScope, QlexpressFunctionScript script) {
        if (script == null || isBlank(script.getFunctionName()) || isBlank(script.getScriptBody())) {
            throw new IllegalStateException("QLExpress 函数配置不能为空");
        }
        String scope = isBlank(runnerScope) ? firstScope(script) : runnerScope;
        BatchAddFunctionResult result = runner.addFunctionsDefinedInScript(
                script.getScriptBody(),
                new MapExpressContext(contextEnhancer.enhance(scope, Collections.emptyMap())),
                QLOptions.DEFAULT_OPTIONS
        );
        List<String> success = result == null ? Collections.emptyList() : result.getSucc();
        List<String> fail = result == null ? Collections.emptyList() : result.getFail();
        if (success == null || success.size() != 1 || !success.contains(script.getFunctionName()) || (fail != null && !fail.isEmpty())) {
            throw new IllegalStateException("QLExpress 函数脚本必须且只能定义函数 " + script.getFunctionName()
                    + "，实际成功=" + success + "，失败=" + fail);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstScope(QlexpressFunctionScript script) {
        List<String> modules = script.getSourceModules();
        return modules == null || modules.isEmpty() ? null : modules.get(0);
    }
}
