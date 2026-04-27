package com.yss.valset.transfer.infrastructure.gateway;

import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.infrastructure.convertor.TransferDeliveryRecordMapper;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferDeliveryRecordPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferDeliveryRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferDeliveryGatewayImplTest {

    @Test
    void shouldStoreCompactRequestAndResponseSnapshots() {
        TransferDeliveryRecordRepository repository = mock(TransferDeliveryRecordRepository.class);
        TransferRouteGateway routeGateway = mock(TransferRouteGateway.class);
        TransferJsonMapper transferJsonMapper = mock(TransferJsonMapper.class);
        TransferDeliveryRecordMapper transferDeliveryRecordMapper = mock(TransferDeliveryRecordMapper.class);

        when(transferJsonMapper.toJson(any())).thenReturn("{}");
        when(routeGateway.findById("route-1")).thenReturn(Optional.of(new TransferRoute(
                "route-1",
                "source-1",
                SourceType.EMAIL,
                "source-code",
                "rule-1",
                TargetType.FILESYS,
                "endpoint-1",
                null,
                "/inbox",
                "rename-{name}",
                true,
                TransferStatus.IDENTIFIED,
                Map.of("triggerType", "MANUAL", "maxRetryCount", 3, "retryDelaySeconds", 60)
        )));

        TransferDeliveryGatewayImpl gateway = new TransferDeliveryGatewayImpl(
                repository,
                routeGateway,
                transferJsonMapper,
                transferDeliveryRecordMapper
        );

        TransferResult result = new TransferResult(false, null, List.of("message-1", "message-2", "message-3", "message-4"));
        gateway.recordResult("route-1", "transfer-1", result, 2);

        ArgumentCaptor<Object> snapshotCaptor = ArgumentCaptor.forClass(Object.class);
        verify(transferJsonMapper, times(2)).toJson(snapshotCaptor.capture());
        List<Object> snapshots = snapshotCaptor.getAllValues();

        assertThat(snapshots).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> requestSnapshot = (Map<String, Object>) snapshots.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseSnapshot = (Map<String, Object>) snapshots.get(1);

        assertThat(requestSnapshot).containsEntry("routeId", "route-1");
        assertThat(requestSnapshot).containsEntry("ruleId", "rule-1");
        assertThat(requestSnapshot).containsEntry("targetCode", "endpoint-1");
        assertThat(requestSnapshot).containsEntry("retryCount", 2);
        assertThat(requestSnapshot).doesNotContainKey("routeMeta");

        assertThat(responseSnapshot).containsEntry("success", false);
        assertThat(responseSnapshot).containsEntry("fileId", null);
        assertThat(responseSnapshot).containsEntry("messageCount", 4);
        assertThat(responseSnapshot).containsEntry("truncated", true);
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) responseSnapshot.get("messages");
        assertThat(messages).containsExactly("message-1", "message-2", "message-3");

        ArgumentCaptor<TransferDeliveryRecordPO> poCaptor = ArgumentCaptor.forClass(TransferDeliveryRecordPO.class);
        verify(repository).insert(poCaptor.capture());
        assertThat(poCaptor.getValue().getRequestSnapshotJson()).isEqualTo("{}");
        assertThat(poCaptor.getValue().getResponseSnapshotJson()).isEqualTo("{}");
    }
}
