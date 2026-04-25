package com.yss.valset.transfer.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 文件分拣调度 cron 适配测试。
 */
class DbTransferJobSchedulerTest {

    @Test
    void normalizeCronExpressionShouldKeepDbSchedulerCron() {
        assertEquals("0 */5 * * * ?", DbTransferJobScheduler.normalizeCronExpression("0 */5 * * * ?"));
    }

    @Test
    void normalizeCronExpressionShouldDropWildcardQuartzYearField() {
        assertEquals("0 */5 * * * ?", DbTransferJobScheduler.normalizeCronExpression("0 */5 * * * ? *"));
    }

    @Test
    void normalizeCronExpressionShouldRejectQuartzYearFieldWithConcreteYear() {
        assertThrows(IllegalArgumentException.class,
                () -> DbTransferJobScheduler.normalizeCronExpression("0 */5 * * * ? 2026"));
    }

    @Test
    void normalizeCronExpressionShouldRejectUnsupportedParts() {
        assertThrows(IllegalArgumentException.class,
                () -> DbTransferJobScheduler.normalizeCronExpression("0 */5 * * *"));
    }
}
