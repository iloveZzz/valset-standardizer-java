package com.yss.valset.analysis.application.impl.management;

import com.yss.valset.analysis.application.command.ParseQueueBackfillCommand;
import com.yss.valset.analysis.application.command.ParseQueueGenerateCommand;
import com.yss.valset.analysis.application.command.ParseQueueRetryCommand;
import com.yss.valset.analysis.application.dto.ParseQueueViewDTO;
import com.yss.valset.analysis.domain.gateway.ParseQueueGateway;
import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.analysis.domain.model.ParseStatus;
import com.yss.valset.analysis.domain.model.ParseTriggerMode;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultParseQueueManagementAppServiceTest {

    @Test
    void generateQueueShouldCreatePendingQueueForEligibleObject() {
        ServiceFixture fixture = newService();
        TransferObjectGateway transferObjectGateway = fixture.transferObjectGateway();
        TransferDeliveryGateway transferDeliveryGateway = fixture.transferDeliveryGateway();
        TransferObjectTagGateway transferObjectTagGateway = fixture.transferObjectTagGateway();
        ParseQueueGateway transferParseQueueGateway = fixture.transferParseQueueGateway();

        TransferObject transferObject = identifiedObject("transfer-1", "source-1", "source-code-1", "route-1", "原始文件.xlsx");
        TransferDeliveryRecord deliveryRecord = deliveredRecord("delivery-1", "route-1", "transfer-1");
        TransferObjectTag valuationTag = valuationTag("tag-1", "transfer-1");

        when(transferObjectGateway.findById("transfer-1")).thenReturn(Optional.of(transferObject));
        when(transferDeliveryGateway.listRecordsByTransferIds(List.of("transfer-1"), "SUCCESS")).thenReturn(List.of(deliveryRecord));
        when(transferObjectTagGateway.listByTransferId("transfer-1")).thenReturn(List.of(valuationTag));
        when(transferParseQueueGateway.findByBusinessKey("transfer-1:VALUATION_TABLE")).thenReturn(Optional.empty());
        when(transferParseQueueGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.transferJsonMapper().toJson(any())).thenReturn("{\"ok\":true}");

        ParseQueueGenerateCommand command = new ParseQueueGenerateCommand();
        command.setTransferId("transfer-1");

        ParseQueueViewDTO result = fixture.service().generateQueue(command);

        assertThat(result.getTransferId()).isEqualTo("transfer-1");
        assertThat(result.getBusinessKey()).isEqualTo("transfer-1:VALUATION_TABLE");
        assertThat(result.getParseStatus()).isEqualTo(ParseStatus.PENDING.name());
        assertThat(result.getTriggerMode()).isEqualTo(ParseTriggerMode.MANUAL.name());
        assertThat(result.getFileStatus()).isEqualTo("已识别");
        assertThat(result.getDeliveryStatus()).isEqualTo("已投递");
        assertThat(result.getRetryCount()).isZero();

        verify(transferParseQueueGateway, times(1)).save(any());
    }

    @Test
    void backfillQueuesShouldCreateMissingQueuesAndKeepExistingQueue() {
        ServiceFixture fixture = newService();
        TransferObjectGateway transferObjectGateway = fixture.transferObjectGateway();
        TransferDeliveryGateway transferDeliveryGateway = fixture.transferDeliveryGateway();
        TransferObjectTagGateway transferObjectTagGateway = fixture.transferObjectTagGateway();
        ParseQueueGateway transferParseQueueGateway = fixture.transferParseQueueGateway();

        TransferObject first = identifiedObject("transfer-1", "source-1", "source-code-1", "route-1", "文件1.xlsx");
        TransferObject second = identifiedObject("transfer-2", "source-1", "source-code-1", "route-1", "文件2.xlsx");
        TransferDeliveryRecord firstDelivery = deliveredRecord("delivery-1", "route-1", "transfer-1");
        TransferDeliveryRecord secondDelivery = deliveredRecord("delivery-2", "route-1", "transfer-2");
        TransferObjectTag firstTag = valuationTag("tag-1", "transfer-1");
        TransferObjectTag secondTag = valuationTag("tag-2", "transfer-2");
        ParseQueue existingQueue = parseQueue("101", "transfer-2", "transfer-2:VALUATION_TABLE", ParseStatus.PENDING, ParseTriggerMode.AUTO);

        when(transferObjectGateway.listParseQueueCandidates(null, null, null, "IDENTIFIED", "DELIVERED", null))
                .thenReturn(List.of(first, second));
        when(transferDeliveryGateway.listRecordsByTransferIds(anyList(), eq("SUCCESS")))
                .thenAnswer(invocation -> {
                    List<String> transferIds = invocation.getArgument(0);
                    if (transferIds == null || transferIds.isEmpty()) {
                        return List.of();
                    }
                    if (transferIds.size() == 1 && "transfer-1".equals(transferIds.get(0))) {
                        return List.of(firstDelivery);
                    }
                    if (transferIds.size() == 1 && "transfer-2".equals(transferIds.get(0))) {
                        return List.of(secondDelivery);
                    }
                    if (transferIds.contains("transfer-1") && transferIds.contains("transfer-2")) {
                        return List.of(firstDelivery, secondDelivery);
                    }
                    return List.of();
                });
        when(transferObjectTagGateway.listByTransferIds(List.of("transfer-1", "transfer-2")))
                .thenReturn(List.of(firstTag, secondTag));
        when(transferParseQueueGateway.findByBusinessKey("transfer-1:VALUATION_TABLE")).thenReturn(Optional.empty());
        when(transferParseQueueGateway.findByBusinessKey("transfer-2:VALUATION_TABLE")).thenReturn(Optional.of(existingQueue));
        when(transferParseQueueGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.transferJsonMapper().toJson(any())).thenReturn("{\"ok\":true}");

        ParseQueueBackfillCommand command = new ParseQueueBackfillCommand();
        command.setDryRun(Boolean.FALSE);
        command.setForceRebuild(Boolean.FALSE);

        List<ParseQueueViewDTO> results = fixture.service().backfillQueues(command);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTransferId()).isEqualTo("transfer-1");
        assertThat(results.get(0).getBusinessKey()).isEqualTo("transfer-1:VALUATION_TABLE");
        assertThat(results.get(1).getTransferId()).isEqualTo("transfer-2");
        assertThat(results.get(1).getBusinessKey()).isEqualTo("transfer-2:VALUATION_TABLE");
        verify(transferParseQueueGateway, times(1)).save(any());
    }

    @Test
    void retryQueueShouldResetFailedQueueToPending() {
        ServiceFixture fixture = newService();
        ParseQueueGateway transferParseQueueGateway = fixture.transferParseQueueGateway();

        ParseQueue failedQueue = new ParseQueue(
                "201",
                "transfer-3:VALUATION_TABLE",
                "transfer-3",
                "原始文件.xlsx",
                "source-1",
                SourceType.EMAIL.name(),
                "source-code-1",
                "route-1",
                "delivery-1",
                "tag-1",
                "VALUATION_TABLE",
                "估值表",
                "IDENTIFIED",
                "DELIVERED",
                ParseStatus.FAILED,
                ParseTriggerMode.MANUAL,
                2,
                "worker-1",
                Instant.parse("2026-04-28T10:00:00Z"),
                Instant.parse("2026-04-28T10:05:00Z"),
                "解析失败",
                "{}",
                "{}",
                "{}",
                "{}",
                Instant.parse("2026-04-28T10:00:00Z"),
                Instant.parse("2026-04-28T10:00:01Z")
        );

        when(transferParseQueueGateway.findById("201")).thenReturn(Optional.of(failedQueue));
        when(transferParseQueueGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ParseQueueRetryCommand command = new ParseQueueRetryCommand();
        command.setForceRebuild(Boolean.FALSE);

        ParseQueueViewDTO result = fixture.service().retryQueue("201", command);

        assertThat(result.getParseStatus()).isEqualTo(ParseStatus.PENDING.name());
        assertThat(result.getRetryCount()).isEqualTo(2);
        assertThat(result.getSubscribedBy()).isNull();
        assertThat(result.getSubscribedAt()).isNull();
        assertThat(result.getParsedAt()).isNull();
        assertThat(result.getLastErrorMessage()).isNull();
        verify(transferParseQueueGateway, times(1)).save(any());
    }

    private ServiceFixture newService() {
        ParseQueueGateway transferParseQueueGateway = mock(ParseQueueGateway.class);
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferJsonMapper transferJsonMapper = mock(TransferJsonMapper.class);
        DefaultParseQueueManagementAppService service = new DefaultParseQueueManagementAppService(
                transferParseQueueGateway,
                transferObjectGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferJsonMapper,
                null
        );
        return new ServiceFixture(service, transferParseQueueGateway, transferObjectGateway, transferObjectTagGateway, transferDeliveryGateway, transferJsonMapper);
    }

    private TransferObject identifiedObject(String transferId, String sourceId, String sourceCode, String routeId, String originalName) {
        return new TransferObject(
                transferId,
                sourceId,
                SourceType.EMAIL.name(),
                sourceCode,
                originalName,
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                1024L,
                "fingerprint-" + transferId,
                "source-ref-" + transferId,
                "mail-" + transferId,
                "sender@example.com",
                "receiver@example.com",
                null,
                null,
                "主题",
                "正文",
                "imap",
                "INBOX",
                "/tmp/" + transferId + ".xlsx",
                TransferStatus.IDENTIFIED,
                Instant.parse("2026-04-28T09:00:00Z"),
                Instant.parse("2026-04-28T09:01:00Z"),
                routeId,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of("fileName", originalName)
        );
    }

    private TransferDeliveryRecord deliveredRecord(String deliveryId, String routeId, String transferId) {
        return new TransferDeliveryRecord(
                deliveryId,
                routeId,
                transferId,
                "FTP",
                "target-1",
                "SUCCESS",
                0,
                "{\"request\":true}",
                "{\"response\":true}",
                null,
                LocalDateTime.parse("2026-04-28T10:00:00")
        );
    }

    private TransferObjectTag valuationTag(String tagId, String transferId) {
        return new TransferObjectTag(
                tagId,
                transferId,
                "VALUATION_TABLE",
                "VALUATION_TABLE",
                "估值表",
                "估值表",
                "AUTO",
                "matched",
                "tagCode",
                "VALUATION_TABLE",
                Map.of("match", "valuation"),
                Instant.parse("2026-04-28T10:00:00Z")
        );
    }

    private ParseQueue parseQueue(String queueId,
                                          String transferId,
                                          String businessKey,
                                          ParseStatus parseStatus,
                                          ParseTriggerMode triggerMode) {
        return new ParseQueue(
                queueId,
                businessKey,
                transferId,
                "原始文件.xlsx",
                "source-1",
                SourceType.EMAIL.name(),
                "source-code-1",
                "route-1",
                "delivery-1",
                "tag-1",
                "VALUATION_TABLE",
                "估值表",
                "IDENTIFIED",
                "DELIVERED",
                parseStatus,
                triggerMode,
                0,
                null,
                null,
                null,
                null,
                "{}",
                "{}",
                "{}",
                "{}",
                Instant.parse("2026-04-28T10:00:00Z"),
                Instant.parse("2026-04-28T10:00:01Z")
        );
    }

    private record ServiceFixture(
            DefaultParseQueueManagementAppService service,
            ParseQueueGateway transferParseQueueGateway,
            TransferObjectGateway transferObjectGateway,
            TransferObjectTagGateway transferObjectTagGateway,
            TransferDeliveryGateway transferDeliveryGateway,
            TransferJsonMapper transferJsonMapper
    ) {
    }
}
