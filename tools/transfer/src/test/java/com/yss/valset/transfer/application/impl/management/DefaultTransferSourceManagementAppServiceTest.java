package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferSourceUpsertCommand;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.infrastructure.convertor.TransferSecretCodec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultTransferSourceManagementAppServiceTest {

    @Test
    void upsertSourceShouldRejectEnabledToggleWhenEnabledRoutesExist() {
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferSourceCheckpointGateway transferSourceCheckpointGateway = mock(TransferSourceCheckpointGateway.class);
        TransferSecretCodec transferSecretCodec = mock(TransferSecretCodec.class);
        TransferSourceScheduleCoordinator transferSourceScheduleCoordinator = mock(TransferSourceScheduleCoordinator.class);
        com.yss.valset.transfer.application.port.TransferJobScheduler transferJobScheduler =
                mock(com.yss.valset.transfer.application.port.TransferJobScheduler.class);
        DefaultTransferSourceManagementAppService service = new DefaultTransferSourceManagementAppService(
                transferSourceGateway,
                transferRouteGateway,
                transferSourceCheckpointGateway,
                transferSecretCodec,
                transferSourceScheduleCoordinator,
                transferJobScheduler
        );

        TransferSource existing = new TransferSource(
                "1",
                "source-1",
                "来源1",
                SourceType.EMAIL,
                true,
                null,
                Map.of(),
                Map.of(),
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        when(transferSourceGateway.findById("1")).thenReturn(Optional.of(existing));
        when(transferRouteGateway.countEnabledBySourceId("1")).thenReturn(2L);

        TransferSourceUpsertCommand command = new TransferSourceUpsertCommand();
        command.setSourceId("1");
        command.setSourceCode("source-1");
        command.setSourceName("来源1");
        command.setSourceType(SourceType.EMAIL.name());
        command.setEnabled(false);
        command.setConnectionConfig(Map.of());
        command.setSourceMeta(Map.of());

        assertThatThrownBy(() -> service.upsertSource(command))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode().value())
                            .isEqualTo(HttpStatus.BAD_REQUEST.value());
                })
                .hasMessageContaining("无法启用或停用来源");

        verifyNoInteractions(transferSourceScheduleCoordinator, transferJobScheduler, transferSourceCheckpointGateway);
    }
}
