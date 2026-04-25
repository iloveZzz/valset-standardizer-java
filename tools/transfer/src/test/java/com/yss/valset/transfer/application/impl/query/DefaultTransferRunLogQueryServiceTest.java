package com.yss.valset.transfer.application.impl.query;

import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunLogPage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTransferRunLogQueryServiceTest {

    @Test
    void shouldExposeHumanReadableRunStatusLabel() {
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferTargetGateway transferTargetGateway = mock(TransferTargetGateway.class);
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferRunLogQueryService service = new DefaultTransferRunLogQueryService(
                transferRunLogGateway,
                transferRouteGateway,
                transferSourceGateway,
                transferTargetGateway,
                transferObjectGateway
        );

        TransferRunLog runLog = new TransferRunLog(
                "run-log-1",
                "source-1",
                "EMAIL",
                "source-code",
                "来源名称",
                "transfer-1",
                null,
                "MANUAL",
                "DELIVER",
                "FAILED",
                "说明",
                "错误",
                LocalDateTime.now()
        );
        when(transferRunLogGateway.listLogs(null, null, null, null, null, null, null))
                .thenReturn(List.of(runLog));

        assertThat(service.listLogs(null, null, null, null, null, null, null))
                .first()
                .extracting("runStatusLabel")
                .isEqualTo("失败");
    }

    @Test
    void pageLogsShouldExposeRunStatusLabelInDetailView() {
        TransferRunLogGateway transferRunLogGateway = mock(TransferRunLogGateway.class);
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferTargetGateway transferTargetGateway = mock(TransferTargetGateway.class);
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferRunLogQueryService service = new DefaultTransferRunLogQueryService(
                transferRunLogGateway,
                transferRouteGateway,
                transferSourceGateway,
                transferTargetGateway,
                transferObjectGateway
        );

        TransferRunLogPage page = new TransferRunLogPage(
                List.of(
                        new TransferRunLog(
                                "run-log-1",
                                "source-1",
                                "EMAIL",
                                "source-code",
                                "来源名称",
                                "transfer-1",
                                null,
                                "MANUAL",
                                "INGEST",
                                "SUCCESS",
                                "说明",
                                null,
                                LocalDateTime.now()
                        )
                ),
                1L,
                0L,
                10
        );
        when(transferRunLogGateway.pageLogs(null, null, null, null, null, null, null, 0, 10))
                .thenReturn(page);

        assertThat(service.pageLogs(null, null, null, null, null, null, null, 0, 10).getData())
                .first()
                .extracting("runStatusLabel")
                .isEqualTo("成功");
    }
}
