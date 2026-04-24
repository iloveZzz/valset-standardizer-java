package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.plugin.TransferActionPlugin;
import com.yss.valset.transfer.infrastructure.plugin.TransferActionPluginRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultDeliverTransferServiceTest {

    @Test
    void executeShouldClearFailedDeliverLogsAfterSuccessfulDelivery() {
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferTargetGateway transferTargetGateway = mock(TransferTargetGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        TransferActionPlugin transferActionPlugin = mock(TransferActionPlugin.class);
        TransferJobScheduler transferJobScheduler = mock(TransferJobScheduler.class);
        ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider = mock(ObjectProvider.class);

        when(transferJobSchedulerProvider.getObject()).thenReturn(transferJobScheduler);
        when(transferRouteGateway.findById("3001")).thenReturn(Optional.of(buildRoute()));
        when(transferObjectGateway.findById("1001")).thenReturn(Optional.of(buildTransferObject()));
        when(transferTargetGateway.findByTargetCode("filesys-archive")).thenReturn(Optional.of(buildTarget()));
        when(transferDeliveryGateway.countByRouteId("3001")).thenReturn(0L);
        when(transferActionPlugin.supports(any())).thenReturn(true);
        when(transferActionPlugin.priority()).thenReturn(0);
        when(transferActionPlugin.execute(any())).thenReturn(new TransferResult(true, List.of("ok")));

        DefaultDeliverTransferService service = new DefaultDeliverTransferService(
                transferRouteGateway,
                transferObjectGateway,
                transferTargetGateway,
                transferDeliveryGateway,
                new TransferActionPluginRegistry(List.of(transferActionPlugin)),
                transferRunLogGateway,
                transferJobSchedulerProvider
        );

        service.execute("3001", "1001");

        var inOrder = inOrder(transferDeliveryGateway, transferRunLogGateway);
        inOrder.verify(transferDeliveryGateway).recordResult(eq("3001"), eq("1001"), any(TransferResult.class), eq(0));
        inOrder.verify(transferRunLogGateway).deleteFailedDeliverLogsByTransferId("1001");
        verifyNoInteractions(transferJobScheduler);
        verify(transferRunLogGateway, org.mockito.Mockito.times(2)).save(any(TransferRunLog.class));
    }

    private TransferObject buildTransferObject() {
        return new TransferObject(
                "1001",
                "2001",
                SourceType.EMAIL.name(),
                "qq-mail",
                "report.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                128L,
                "fingerprint-1",
                "mail-1",
                "mail-1",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/report.xlsx",
                TransferStatus.RECEIVED,
                Instant.parse("2026-04-24T00:00:00Z"),
                Instant.parse("2026-04-24T00:01:00Z"),
                null,
                null,
                null,
                Map.of("sourceCode", "qq-mail", "triggerType", "MANUAL")
        );
    }

    private TransferRoute buildRoute() {
        return new TransferRoute(
                "3001",
                "2001",
                SourceType.EMAIL,
                "qq-mail",
                "4001",
                TargetType.FILESYS,
                "filesys-archive",
                "/transfer/inbox",
                "${fileName}",
                TransferStatus.IDENTIFIED,
                Map.of()
        );
    }

    private TransferTarget buildTarget() {
        return new TransferTarget(
                4001L,
                "filesys-archive",
                "文件系统归档",
                TargetType.FILESYS,
                true,
                "/transfer/inbox/${fileName}",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-24T00:00:00Z"),
                Instant.parse("2026-04-24T00:00:00Z")
        );
    }
}
