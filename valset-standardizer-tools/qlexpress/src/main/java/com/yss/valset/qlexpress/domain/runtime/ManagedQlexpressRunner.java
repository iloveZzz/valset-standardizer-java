package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 可整体替换的 QLExpress Runner。
 */
public class ManagedQlexpressRunner implements QlexpressRunnerHolder {

    @FunctionalInterface
    public interface RunnerHandle {
        Object execute(String expression, Map<String, Object> context, QLOptions options) throws Exception;
    }

    private final Supplier<Express4Runner> supplier;
    private final int maxConcurrency;
    private final Supplier<RunnerHandle> handleSupplier;

    private volatile BlockingQueue<Express4Runner> runnerPool;
    private volatile BlockingQueue<RunnerHandle> handlePool;

    public ManagedQlexpressRunner(Supplier<Express4Runner> supplier, int maxConcurrency) {
        this(supplier, maxConcurrency, null);
    }

    public ManagedQlexpressRunner(Supplier<Express4Runner> supplier, int maxConcurrency, Supplier<RunnerHandle> handleSupplier) {
        this.supplier = supplier;
        this.maxConcurrency = Math.max(1, maxConcurrency);
        this.handleSupplier = handleSupplier;
        this.runnerPool = handleSupplier == null ? buildRunnerPool() : null;
        this.handlePool = buildHandlePool();
    }

    @Override
    public Express4Runner getRunner() {
        BlockingQueue<Express4Runner> currentPool = runnerPool;
        Express4Runner current = currentPool.peek();
        return current == null ? supplier.get() : current;
    }

    /**
     * 托管 runner 使用有限并发池，每次执行借用一个独立 runner 实例。
     */
    public Object executeResult(String expression, Map<String, Object> context, QLOptions options) {
        if (handleSupplier != null) {
            BlockingQueue<RunnerHandle> currentHandlePool = handlePool;
            RunnerHandle currentHandle = null;
            try {
                currentHandle = currentHandlePool.take();
                return currentHandle.execute(expression, context, options);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("QLExpress runner 被中断", interruptedException);
            } catch (Exception exception) {
                throw new IllegalStateException("QLExpress runner 执行失败", exception);
            } finally {
                if (currentHandle != null) {
                    currentHandlePool.offer(currentHandle);
                }
            }
        }
        BlockingQueue<Express4Runner> currentPool = runnerPool;
        Express4Runner current = null;
        try {
            current = currentPool.take();
            return current.execute(expression, context, options).getResult();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("QLExpress runner 被中断", interruptedException);
        } finally {
            if (current != null) {
                currentPool.offer(current);
            }
        }
    }

    @Override
    public synchronized void refresh() {
        this.runnerPool = handleSupplier == null ? buildRunnerPool() : null;
        this.handlePool = buildHandlePool();
    }

    private BlockingQueue<Express4Runner> buildRunnerPool() {
        BlockingQueue<Express4Runner> pool = new ArrayBlockingQueue<>(maxConcurrency, true);
        for (int i = 0; i < maxConcurrency; i++) {
            pool.offer(supplier.get());
        }
        return pool;
    }

    private BlockingQueue<RunnerHandle> buildHandlePool() {
        if (handleSupplier == null) {
            return null;
        }
        BlockingQueue<RunnerHandle> pool = new ArrayBlockingQueue<>(maxConcurrency, true);
        for (int i = 0; i < maxConcurrency; i++) {
            pool.offer(handleSupplier.get());
        }
        return pool;
    }
}
