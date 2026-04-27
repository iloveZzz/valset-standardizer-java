package com.yss.valset.transfer.application.impl.deliver;

import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.plugin.TransferActionPlugin;
import com.yss.valset.transfer.infrastructure.plugin.TransferActionPluginRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDeliverTransferServiceTest {

    @Test
    void shouldPersistRealStoragePathAfterSuccessfulDeliver() {
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferTargetGateway transferTargetGateway = mock(TransferTargetGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferActionPluginRegistry transferActionPluginRegistry = mock(TransferActionPluginRegistry.class);
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider = mock(ObjectProvider.class);
        TransferJobScheduler transferJobScheduler = mock(TransferJobScheduler.class);
        TransferActionPlugin transferActionPlugin = mock(TransferActionPlugin.class);
        when(transferJobSchedulerProvider.getObject()).thenReturn(transferJobScheduler);

        DefaultDeliverTransferService service = new DefaultDeliverTransferService(
                transferRouteGateway,
                transferObjectGateway,
                transferTargetGateway,
                transferDeliveryGateway,
                transferActionPluginRegistry,
                transferRunLogGateway,
                transferJobSchedulerProvider
        );

        TransferRoute route = new TransferRoute(
                "route-1",
                "source-1",
                SourceType.EMAIL,
                "source-code",
                "rule-1",
                TargetType.LOCAL_DIR,
                "target-code",
                null,
                "/archive/${yyyyMMdd}",
                null,
                true,
                TransferStatus.ROUTED,
                Map.of()
        );
        TransferTarget target = new TransferTarget(
                1L,
                "target-code",
                "本地目录",
                TargetType.LOCAL_DIR,
                true,
                "/archive/${yyyyMMdd}",
                Map.of(),
                Map.of(),
                Instant.parse("2026-04-27T00:00:00Z"),
                Instant.parse("2026-04-27T00:00:00Z")
        );
        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                SourceType.EMAIL.name(),
                "source-code",
                "report.csv",
                "csv",
                "text/csv",
                128L,
                "fingerprint-1",
                "source-ref-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "/tmp/report.csv",
                TransferStatus.RECEIVED,
                Instant.parse("2026-04-27T00:00:00Z"),
                Instant.parse("2026-04-27T00:01:00Z"),
                "route-1",
                null,
                null,
                Map.of()
        );

        TransferResult result = new TransferResult(true, null, "/real/archive/report.csv", List.of("ok"));

        when(transferRouteGateway.findById("route-1")).thenReturn(java.util.Optional.of(route));
        when(transferObjectGateway.findById("transfer-1")).thenReturn(java.util.Optional.of(transferObject));
        when(transferTargetGateway.findByTargetCode("target-code")).thenReturn(java.util.Optional.of(target));
        when(transferDeliveryGateway.countByRouteId("route-1")).thenReturn(0L);
        when(transferActionPluginRegistry.getRequired(route)).thenReturn(transferActionPlugin);
        when(transferActionPlugin.execute(any())).thenReturn(result);
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute("route-1", "transfer-1");

        verify(transferObjectGateway).save(argThat(savedObject ->
                savedObject != null && "/real/archive/report.csv".equals(savedObject.realStoragePath())
        ));
        verify(transferActionPlugin).execute(any());
    }
}
