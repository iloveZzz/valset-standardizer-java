package com.yss.valset.batch.scheduler;

/**
 * 批处理调度 cron 工具。
 */
public final class BatchSchedulerCronSupport {

    private static final String DEFAULT_CRON = "0 0/5 * * * ?";

    private BatchSchedulerCronSupport() {
    }

    public static String normalizeCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return DEFAULT_CRON;
        }
        String normalized = cronExpression.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(" ");
        if (parts.length == 6) {
            return normalized;
        }
        if (parts.length == 7) {
            String yearPart = parts[6];
            if ("*".equals(yearPart) || "?".equals(yearPart)) {
                return String.join(" ", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            }
            throw new IllegalArgumentException("cron 表达式包含年份字段，db-scheduler 不支持 7 段 cron：" + cronExpression);
        }
        throw new IllegalArgumentException("cron 表达式必须为 6 段或兼容的 7 段 Quartz cron，当前为 " + parts.length + " 段：" + cronExpression);
    }
}
