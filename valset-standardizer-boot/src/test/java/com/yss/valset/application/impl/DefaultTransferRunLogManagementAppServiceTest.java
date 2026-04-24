package com.yss.valset.application.impl;

import com.yss.valset.application.command.TransferRunLogRedeliverCommand;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferRunLogManagementAppServiceTest {

    @Test
    void redeliverShouldOnlyExecuteFailedDeliverLogsAndDeduplicatePairs() {
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        TransferProcessUseCase transferProcessUseCase = mock(TransferProcessUseCase.class);
        DefaultTransferRunLogManagementAppService service = new DefaultTransferRunLogManagementAppService(
                transferRunLogGateway,
                transferProcessUseCase
        );

        when(transferRunLogGateway.findById("1")).thenReturn(Optional.of(buildRunLog("1", "10", "20", "DELIVER", "FAILED")));
        when(transferRunLogGateway.findById("2")).thenReturn(Optional.of(buildRunLog("2", "10", "20", "DELIVER", "FAILED")));
        when(transferRunLogGateway.findById("3")).thenReturn(Optional.of(buildRunLog("3", "11", "21", "ROUTE", "FAILED")));

        TransferRunLogRedeliverCommand command = new TransferRunLogRedeliverCommand();
        command.setRunLogIds(List.of("1", "2", "3"));
        var response = service.redeliver(command);

        assertThat(response.getRequestedCount()).isEqualTo(3);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(0);
        assertThat(response.getSkippedCount()).isEqualTo(2);
        assertThat(response.getItems()).hasSize(3);
        verify(transferProcessUseCase).deliver("20", "10");
    }

    @Test
    void redeliverShouldRejectEmptySelection() {
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        TransferProcessUseCase transferProcessUseCase = mock(TransferProcessUseCase.class);
        DefaultTransferRunLogManagementAppService service = new DefaultTransferRunLogManagementAppService(
                transferRunLogGateway,
                transferProcessUseCase
        );

        assertThatThrownBy(() -> service.redeliver(new TransferRunLogRedeliverCommand()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("请选择需要重新投递的运行日志");
    }

    private TransferRunLog buildRunLog(String runLogId,
                                       String transferId,
                                       String routeId,
                                       String runStage,
                                       String runStatus) {
        return new TransferRunLog(
                runLogId,
                "1",
                "EMAIL",
                "email-daily",
                "日报邮箱",
                transferId,
                routeId,
                "MANUAL",
                runStage,
                runStatus,
                "日志说明",
                null,
                LocalDateTime.now()
        );
    }
}
