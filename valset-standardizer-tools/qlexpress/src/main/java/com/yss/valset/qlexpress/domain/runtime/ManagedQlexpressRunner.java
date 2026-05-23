package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;

import java.util.Map;

import java.util.function.Supplier;

/**
 * 可整体替换的 QLExpress Runner。
 */
public class ManagedQlexpressRunner implements QlexpressRunnerHolder {

    private final Supplier<Express4Runner> supplier;

    private volatile Express4Runner runner;

    public ManagedQlexpressRunner(Supplier<Express4Runner> supplier) {
        this.supplier = supplier;
        this.runner = supplier.get();
    }

    @Override
    public Express4Runner getRunner() {
        return runner;
    }

    /**
     * Express4Runner 在解析执行过程中会维护内部状态，托管入口统一串行化单个 runner 的执行。
     */
    public synchronized Object executeResult(String expression, Map<String, Object> context, QLOptions options) {
        return runner.execute(expression, context, options).getResult();
    }

    @Override
    public synchronized void refresh() {
        this.runner = supplier.get();
    }
}
