package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferRouteUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferRouteMutationResponse;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferSecretCodec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferRouteManagementAppServiceTest {

    @Test
    void upsertRouteShouldResyncBothOldAndNewSourceWhenSourceIdChanges() {
        var transferRouteGateway = mock(com.yss.valset.transfer.domain.gateway.TransferRouteGateway.class);
        var transferSourceScheduleCoordinator = mock(TransferSourceScheduleCoordinator.class);
        var transferSecretCodec = mock(TransferSecretCodec.class);
        var service = new DefaultTransferRouteManagementAppService(
                transferRouteGateway,
                transferSourceScheduleCoordinator,
                transferSecretCodec
        );

        TransferRoute existing = route("10", "1", "source-1", "0 */5 * * * ?");
        TransferRoute saved = route("10", "2", "source-2", "0 */10 * * * ?");
        when(transferRouteGateway.findById("10")).thenReturn(java.util.Optional.of(existing));
        when(transferRouteGateway.save(any())).thenReturn(saved);
        when(transferSecretCodec.maskMap(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRouteUpsertCommand command = new TransferRouteUpsertCommand();
        command.setRouteId("10");
        command.setSourceId("2");
        command.setSourceType(SourceType.EMAIL.name());
        command.setSourceCode("source-2");
        command.setRuleId("rule-1");
        command.setTargetType(TargetType.FILESYS.name());
        command.setTargetCode("endpoint-2");
        command.setEnabled(true);
        command.setPollCron("0 */10 * * * ?");
        command.setRouteStatus(TransferStatus.IDENTIFIED.name());
        command.setRouteMeta(Map.of());

        TransferRouteMutationResponse response = service.upsertRoute(command);

        assertThat(response.getRoute().getSourceId()).isEqualTo("2");
        verify(transferSourceScheduleCoordinator).syncSourceScheduleBySourceId("1");
        verify(transferSourceScheduleCoordinator).syncSourceScheduleBySourceId("2");
    }

    private TransferRoute route(String routeId, String sourceId, String sourceCode, String pollCron) {
        return new TransferRoute(
                routeId,
                sourceId,
                SourceType.EMAIL,
                sourceCode,
                "rule-1",
                TargetType.FILESYS,
                "endpoint-1",
                pollCron,
                "/inbox",
                "rename-{name}",
                true,
                TransferStatus.IDENTIFIED,
                Map.of()
        );
    }
}
