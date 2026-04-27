package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunLogAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogStageAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogStatusCount;
import com.yss.valset.transfer.infrastructure.convertor.TransferRunLogMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferRunLogPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferRunLogRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferRunLogGatewayImplTest {

    @Test
    void pageLogsShouldReturnLatestFailedDeliverResultForSameTransferObject() {
        TransferRunLogRepository repository = mock(TransferRunLogRepository.class);
        TransferRunLogMapper mapper = new TransferRunLogMapper() {
        };
        TransferRunLogGatewayImpl gateway = new TransferRunLogGatewayImpl(repository, mapper);

        when(repository.selectList(any())).thenReturn(List.of(
                buildPo("3", "10", "2026-04-24T10:00:00"),
                buildPo("2", "11", "2026-04-24T09:00:00"),
                buildPo("1", "10", "2026-04-24T08:00:00")
        ));

        var page = gateway.pageLogs(null, null, null, "DELIVER", "FAILED", null, null, 0, 10);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.records()).extracting(TransferRunLog::transferId).containsExactly("10", "11");
        assertThat(page.records()).extracting(TransferRunLog::runLogId).containsExactly("3", "2");
        verify(repository, never()).selectPage(any(IPage.class), any());
    }

    @Test
    void analyzeLogsShouldCountLatestFailedDeliverResultForSameTransferObject() {
        TransferRunLogRepository repository = mock(TransferRunLogRepository.class);
        TransferRunLogMapper mapper = new TransferRunLogMapper() {
        };
        TransferRunLogGatewayImpl gateway = new TransferRunLogGatewayImpl(repository, mapper);

        when(repository.selectList(any())).thenReturn(List.of(
                buildPo("4", "10", "2026-04-24T11:00:00", "SUCCESS"),
                buildPo("3", "10", "2026-04-24T10:00:00", "FAILED"),
                buildPo("2", "11", "2026-04-24T09:00:00"),
                buildPo("1", "11", "2026-04-24T08:00:00", "FAILED")
        ));

        TransferRunLogAnalysis analysis = gateway.analyzeLogs(null, null, null, "DELIVER", "FAILED", null, null);

        TransferRunLogStageAnalysis deliverStage = analysis.stageAnalyses().stream()
                .filter(item -> "DELIVER".equals(item.runStage()))
                .findFirst()
                .orElseThrow();
        assertThat(deliverStage.totalCount()).isEqualTo(2);
        assertThat(analysis.totalCount()).isEqualTo(4);
        assertThat(deliverStage.statusCounts()).extracting(TransferRunLogStatusCount::runStatus, TransferRunLogStatusCount::count)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SUCCESS", 1L),
                        org.assertj.core.groups.Tuple.tuple("FAILED", 1L)
                );
    }

    @Test
    void deleteFailedDeliverLogsByTransferIdShouldDeleteMatchingFailedDeliverRows() {
        TransferRunLogRepository repository = mock(TransferRunLogRepository.class);
        TransferRunLogMapper mapper = new TransferRunLogMapper() {
        };
        TransferRunLogGatewayImpl gateway = new TransferRunLogGatewayImpl(repository, mapper);

        when(repository.delete(any())).thenReturn(2);

        long deletedCount = gateway.deleteFailedDeliverLogsByTransferId("10");

        assertThat(deletedCount).isEqualTo(2L);
        verify(repository).delete(any());
    }

    @Test
    void deleteLogsCreatedBetweenShouldDeleteMatchingRows() {
        TransferRunLogRepository repository = mock(TransferRunLogRepository.class);
        TransferRunLogMapper mapper = new TransferRunLogMapper() {
        };
        TransferRunLogGatewayImpl gateway = new TransferRunLogGatewayImpl(repository, mapper);

        when(repository.delete(any())).thenReturn(3);

        long deletedCount = gateway.deleteLogsCreatedBetween(
                LocalDateTime.parse("2026-04-24T00:00:00"),
                LocalDateTime.parse("2026-04-25T00:00:00")
        );

        assertThat(deletedCount).isEqualTo(3L);
        verify(repository).delete(any());
    }

    private TransferRunLogPO buildPo(String runLogId, String transferId, String createdAt, String runStatus) {
        TransferRunLogPO po = new TransferRunLogPO();
        po.setRunLogId(runLogId);
        po.setSourceId("1");
        po.setSourceType("EMAIL");
        po.setSourceCode("email-daily");
        po.setSourceName("日报邮箱");
        po.setTransferId(transferId);
        po.setRouteId("20");
        po.setTriggerType("MANUAL");
        po.setRunStage("DELIVER");
        po.setRunStatus(runStatus);
        po.setLogMessage("日志说明");
        po.setErrorMessage(null);
        po.setCreatedAt(LocalDateTime.parse(createdAt));
        return po;
    }

    private TransferRunLogPO buildPo(String runLogId, String transferId, String createdAt) {
        return buildPo(runLogId, transferId, createdAt, "FAILED");
    }
}
