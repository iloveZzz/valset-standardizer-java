package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.Express4Runner;

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

    @Override
    public void refresh() {
        this.runner = supplier.get();
    }
}
