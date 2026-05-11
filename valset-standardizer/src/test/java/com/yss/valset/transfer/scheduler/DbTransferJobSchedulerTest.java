package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceCurrentlyExecutingException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void scheduleIngestCronShouldFallbackWhenTaskIsCurrentlyExecuting() {
        SchedulerClient schedulerClient = mock(SchedulerClient.class);
        when(schedulerClient.reschedule(any())).thenThrow(new TaskInstanceCurrentlyExecutingException("transfer-ingest-cron", "2047251110490148865"));
        DbTransferJobScheduler scheduler = new DbTransferJobScheduler(schedulerClient);

        scheduler.scheduleIngestCron("2047251110490148865", "EMAIL", "source-a", Map.of(), "0 0/5 * * * ?");

        verify(schedulerClient).reschedule(any());
    }
}
