package com.yss.valset.domain.rule;

/**
 * 解析规则追踪上下文持有器。
 */
public final class ParseRuleTraceContextHolder {

    private static final ThreadLocal<ParseRuleTraceContext> HOLDER = new ThreadLocal<>();

    private ParseRuleTraceContextHolder() {
    }

    /**
     * 设置当前线程追踪上下文。
     */
    public static void set(ParseRuleTraceContext context) {
        if (context == null) {
            HOLDER.remove();
            return;
        }
        HOLDER.set(context);
    }

    /**
     * 获取当前线程追踪上下文。
     */
    public static ParseRuleTraceContext get() {
        return HOLDER.get();
    }

    /**
     * 清理当前线程追踪上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 判断当前是否需要追踪。
     */
    public static boolean isEnabled() {
        ParseRuleTraceContext context = HOLDER.get();
        return context != null && Boolean.TRUE.equals(context.getTraceEnabled());
    }

    /**
     * 在指定上下文内执行。
     */
    public static TraceScope withContext(ParseRuleTraceContext context) {
        ParseRuleTraceContext previous = HOLDER.get();
        set(context);
        return new TraceScope(previous);
    }

    /**
     * 追踪上下文作用域。
     */
    public static final class TraceScope implements AutoCloseable {
        private final ParseRuleTraceContext previous;

        private TraceScope(ParseRuleTraceContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }
}
