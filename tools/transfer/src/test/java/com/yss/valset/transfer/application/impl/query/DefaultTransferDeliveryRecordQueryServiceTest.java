package com.yss.valset.transfer.application.impl.query;

import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecordPage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTransferDeliveryRecordQueryServiceTest {

    @Test
    void shouldExposeHumanReadableExecuteStatusLabel() {
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        DefaultTransferDeliveryRecordQueryService service = new DefaultTransferDeliveryRecordQueryService(transferDeliveryGateway);

        TransferDeliveryRecord record = new TransferDeliveryRecord(
                "delivery-1",
                "route-1",
                "transfer-1",
                "FILESYS",
                "target-1",
                "SUCCESS",
                0,
                "{}",
                "{}",
                null,
                LocalDateTime.now()
        );
        when(transferDeliveryGateway.findById("delivery-1")).thenReturn(java.util.Optional.of(record));

        assertThat(service.getRecord("delivery-1").getExecuteStatusLabel()).isEqualTo("成功");
    }

    @Test
    void pageRecordsShouldExposeLabelsForMultipleStatuses() {
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        DefaultTransferDeliveryRecordQueryService service = new DefaultTransferDeliveryRecordQueryService(transferDeliveryGateway);

        TransferDeliveryRecordPage page = new TransferDeliveryRecordPage(
                List.of(
                        new TransferDeliveryRecord(
                                "delivery-1",
                                "route-1",
                                "transfer-1",
                                "FILESYS",
                                "target-1",
                                "FAILED",
                                0,
                                "{}",
                                "{}",
                                "error",
                                LocalDateTime.now()
                        )
                ),
                1L,
                0L,
                10
        );
        when(transferDeliveryGateway.pageRecords(null, null, null, null, 0, 10)).thenReturn(page);

        assertThat(service.pageRecords(null, null, null, null, 0, 10).getData())
                .first()
                .extracting("executeStatusLabel")
                .isEqualTo("失败");
    }
}
