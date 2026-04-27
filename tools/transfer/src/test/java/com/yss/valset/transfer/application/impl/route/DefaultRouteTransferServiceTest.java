package com.yss.valset.transfer.application.impl.route;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.MatchResult;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.plugin.FileProbePlugin;
import com.yss.valset.transfer.domain.plugin.RouteMatchPlugin;
import com.yss.valset.transfer.infrastructure.connector.SourceConnectorRegistry;
import com.yss.valset.transfer.infrastructure.plugin.FileProbePluginRegistry;
import com.yss.valset.transfer.infrastructure.plugin.RouteMatchPluginRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultRouteTransferServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldMaterializeBeforeRouteMatchingAndUseStoredPath() throws Exception {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferJobScheduler transferJobScheduler = mock(TransferJobScheduler.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider = mock(ObjectProvider.class);
        SourceConnector sourceConnector = mock(SourceConnector.class);
        FileProbePlugin fileProbePlugin = mock(FileProbePlugin.class);
        RouteMatchPlugin routeMatchPlugin = mock(RouteMatchPlugin.class);

        when(sourceConnector.type()).thenReturn("EMAIL");
        SourceConnectorRegistry sourceConnectorRegistry = new SourceConnectorRegistry(List.of(sourceConnector));
        FileProbePluginRegistry fileProbePluginRegistry = new FileProbePluginRegistry(List.of(fileProbePlugin));
        RouteMatchPluginRegistry routeMatchPluginRegistry = new RouteMatchPluginRegistry(List.of(routeMatchPlugin));

        DefaultRouteTransferService service = new DefaultRouteTransferService(
                transferObjectGateway,
                fileProbePluginRegistry,
                routeMatchPluginRegistry,
                transferRunLogGateway,
                transferJobSchedulerProvider,
                transferSourceGateway,
                sourceConnectorRegistry,
                tempDir.resolve("uploads").toString()
        );

        Path sourceTempFile = tempDir.resolve("source-temp.xls");
        Files.writeString(sourceTempFile, "demo");

        TransferSource source = new TransferSource(
                "source-1",
                "source-code",
                "来源",
                SourceType.EMAIL,
                true,
                "0 0 * * * ?",
                Map.of(),
                Map.of(),
                "RUNNING",
                "SYSTEM",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        TransferObject initialTransferObject = new TransferObject(
                "transfer-1",
                source.sourceId(),
                source.sourceType().name(),
                source.sourceCode(),
                "mail.xls",
                "xls",
                "application/vnd.ms-excel",
                123L,
                "fingerprint",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                null,
                TransferStatus.RECEIVED,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Map.of()
        );

        when(transferObjectGateway.findById("transfer-1")).thenReturn(java.util.Optional.of(initialTransferObject));
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferSourceGateway.findById(source.sourceId())).thenReturn(java.util.Optional.of(source));
        when(transferJobSchedulerProvider.getObject()).thenReturn(transferJobScheduler);
        when(sourceConnector.materialize(any(), any())).thenReturn(sourceTempFile);
        when(fileProbePlugin.priority()).thenReturn(1);
        when(fileProbePlugin.supports(any())).thenReturn(true);
        when(fileProbePlugin.probe(any())).thenReturn(new ProbeResult(false, null, Map.of()));

        TransferRoute route = new TransferRoute(
                "route-1",
                source.sourceId(),
                SourceType.EMAIL,
                source.sourceCode(),
                "rule-1",
                TargetType.FILESYS,
                "endpoint-1",
                null,
                "/inbox",
                null,
                true,
                TransferStatus.IDENTIFIED,
                Map.of()
        );
        when(routeMatchPlugin.priority()).thenReturn(1);
        when(routeMatchPlugin.supports(any())).thenReturn(true);
        when(routeMatchPlugin.match(any(), any())).thenReturn(new MatchResult(true, List.of(route), "命中"));

        service.execute("transfer-1");

        Path storedPath = tempDir.resolve("uploads")
                .toAbsolutePath()
                .resolve(LocalDate.now().toString())
                .resolve("mail.xls");
        assertThat(Files.exists(storedPath)).isTrue();

        var contextCaptor = org.mockito.ArgumentCaptor.forClass(RecognitionContext.class);
        verify(routeMatchPlugin).match(contextCaptor.capture(), any());
        assertThat(contextCaptor.getValue().path()).isEqualTo(storedPath.toString());
        assertThat(contextCaptor.getValue().path()).isNotEqualTo(sourceTempFile.toString());
    }
}
