package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferSourceScheduleCoordinatorTest {

    @Test
    void reconcileAllSourcesOnStartupShouldRestoreEnabledCronAndUnscheduleDisabledSources() {
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferJobScheduler transferJobScheduler = mock(TransferJobScheduler.class);
        TransferSourceScheduleCoordinator coordinator = new TransferSourceScheduleCoordinator(
                transferSourceGateway,
                transferJobScheduler
        );

        TransferSource enabledSource = source("1", true, "0 */5 * * * ?");
        TransferSource disabledSource = source("2", false, "0 */10 * * * ?");
        TransferSource blankCronSource = source("3", true, "   ");
        TransferSource invalidCronSource = source("4", true, "bad cron");

        when(transferSourceGateway.listSources(null, null, null, null, null))
                .thenReturn(List.of(enabledSource, disabledSource, blankCronSource, invalidCronSource));
        doThrow(new IllegalArgumentException("invalid cron"))
                .when(transferJobScheduler)
                .scheduleIngestCron(eq("4"), eq("EMAIL"), eq("source-4"), eq(Map.of()), eq("bad cron"));

        assertThatCode(coordinator::reconcileAllSourcesOnStartup).doesNotThrowAnyException();

        verify(transferJobScheduler).scheduleIngestCron("1", "EMAIL", "source-1", Map.of(), "0 */5 * * * ?");
        verify(transferJobScheduler).unscheduleIngest("2");
        verify(transferJobScheduler).unscheduleIngest("3");
        verify(transferJobScheduler).scheduleIngestCron("4", "EMAIL", "source-4", Map.of(), "bad cron");
    }

    @Test
    void syncSourceScheduleShouldIgnoreNullSource() {
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferJobScheduler transferJobScheduler = mock(TransferJobScheduler.class);
        TransferSourceScheduleCoordinator coordinator = new TransferSourceScheduleCoordinator(
                transferSourceGateway,
                transferJobScheduler
        );

        assertThatCode(() -> coordinator.syncSourceSchedule(null)).doesNotThrowAnyException();
    }

    private TransferSource source(String id, boolean enabled, String pollCron) {
        return new TransferSource(
                id,
                "source-" + id,
                "来源" + id,
                SourceType.EMAIL,
                enabled,
                pollCron,
                Map.of(),
                Map.of(),
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }
}
