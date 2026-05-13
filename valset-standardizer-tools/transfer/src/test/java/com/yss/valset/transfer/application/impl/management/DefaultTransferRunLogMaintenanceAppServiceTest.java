package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferRunLogMaintenanceAppServiceTest {

    @Test
    void cleanupYesterdayLogsShouldDeletePreviousDayLogsOnly() {
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        DefaultTransferRunLogMaintenanceAppService service = new DefaultTransferRunLogMaintenanceAppService(transferRunLogGateway);

        when(transferRunLogGateway.deleteLogsCreatedBetween(any(), any())).thenReturn(7L);

        long deletedCount = service.cleanupYesterdayLogs().getDeletedCount();

        assertThat(deletedCount).isEqualTo(7L);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        verify(transferRunLogGateway).deleteLogsCreatedBetween(
                LocalDateTime.of(today.minusDays(1), java.time.LocalTime.MIDNIGHT),
                LocalDateTime.of(today, java.time.LocalTime.MIDNIGHT)
        );
    }
}
