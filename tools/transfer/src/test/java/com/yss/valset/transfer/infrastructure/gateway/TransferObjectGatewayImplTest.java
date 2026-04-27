package com.yss.valset.transfer.infrastructure.gateway;

import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferMailInfoGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferMailInfo;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.convertor.TransferObjectMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferObjectGatewayImplTest {

    @Test
    void should_save_mail_info_after_insert_uses_persisted_transfer_id() {
        TransferObjectRepository transferObjectRepository = mock(TransferObjectRepository.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferJsonMapper transferJsonMapper = mock(TransferJsonMapper.class);
        TransferObjectMapper transferObjectMapper = mock(TransferObjectMapper.class);

        TransferObjectGatewayImpl gateway = new TransferObjectGatewayImpl(
                transferObjectRepository,
                transferMailInfoGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferJsonMapper,
                transferObjectMapper,null
        );

        TransferObject transferObject = new TransferObject(
                null,
                "source-1",
                "EMAIL",
                "source-code",
                "demo.eml",
                "eml",
                "message/rfc822",
                128L,
                "fingerprint-1",
                "source-ref-1",
                "mail-1",
                "sender@example.com",
                "to@example.com",
                "cc@example.com",
                "bcc@example.com",
                "subject-1",
                "body-1",
                "imap",
                "INBOX",
                null,
                TransferStatus.RECEIVED,
                Instant.parse("2026-04-27T12:00:00Z"),
                Instant.parse("2026-04-27T12:00:01Z"),
                null,
                null,
                new ProbeResult(true, "EMAIL", Map.of()),
                Map.of()
        );

        TransferObjectPO po = new TransferObjectPO();
        when(transferObjectMapper.toPO(transferObject, transferJsonMapper)).thenReturn(po);
        when(transferObjectMapper.toDomain(po, transferJsonMapper)).thenReturn(transferObject);
        doAnswer(invocation -> {
            TransferObjectPO saved = invocation.getArgument(0);
            saved.setTransferId("1001");
            return null;
        }).when(transferObjectRepository).insert(any(TransferObjectPO.class));

        gateway.save(transferObject);

        verify(transferMailInfoGateway).save(org.mockito.ArgumentMatchers.argThat(mailInfo ->
                "1001".equals(mailInfo.transferId())
                        && "mail-1".equals(mailInfo.mailId())
                        && "subject-1".equals(mailInfo.mailSubject())
        ));
        assertThat(po.getTransferId()).isEqualTo("1001");
    }

    @Test
    void should_hydrate_mail_info_for_email_inbox_queries() {
        TransferObjectRepository transferObjectRepository = mock(TransferObjectRepository.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferJsonMapper transferJsonMapper = mock(TransferJsonMapper.class);
        TransferObjectMapper transferObjectMapper = mock(TransferObjectMapper.class);

        TransferObjectGatewayImpl gateway = new TransferObjectGatewayImpl(
                transferObjectRepository,
                transferMailInfoGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferJsonMapper,
                transferObjectMapper,null
        );

        TransferObjectPO firstPo = new TransferObjectPO();
        firstPo.setTransferId("1001");
        TransferObjectPO secondPo = new TransferObjectPO();
        secondPo.setTransferId("1002");
        TransferObject first = new TransferObject(
                "1001",
                "source-1",
                "EMAIL",
                "source-code",
                "a.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                10L,
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
                null,
                TransferStatus.RECEIVED,
                Instant.parse("2026-04-27T12:00:00Z"),
                Instant.parse("2026-04-27T12:00:01Z"),
                null,
                null,
                null,
                Map.of()
        );
        TransferObject second = new TransferObject(
                "1002",
                "source-1",
                "EMAIL",
                "source-code",
                "b.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                11L,
                "fingerprint-2",
                "source-ref-2",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TransferStatus.RECEIVED,
                Instant.parse("2026-04-27T12:00:02Z"),
                Instant.parse("2026-04-27T12:00:03Z"),
                null,
                null,
                null,
                Map.of()
        );

        when(transferObjectRepository.selectList(any())).thenReturn(List.of(firstPo, secondPo));
        when(transferObjectMapper.toDomain(firstPo, transferJsonMapper)).thenReturn(first);
        when(transferObjectMapper.toDomain(secondPo, transferJsonMapper)).thenReturn(second);
        when(transferMailInfoGateway.listByTransferIds(List.of("1001", "1002"))).thenReturn(List.of(
                new TransferMailInfo("1001", "mail-1", "sender@example.com", "to@example.com", null, null, "subject-1", "body-1", "imap", "INBOX"),
                new TransferMailInfo("1002", "mail-1", "sender@example.com", "to@example.com", null, null, "subject-1", "body-1", "imap", "INBOX")
        ));

        List<TransferObject> objects = gateway.listEmailInboxObjects("source-code", null);

        assertThat(objects).hasSize(2);
        assertThat(objects.get(0).mailId()).isEqualTo("mail-1");
        assertThat(objects.get(1).mailId()).isEqualTo("mail-1");
        verify(transferMailInfoGateway).listByTransferIds(List.of("1001", "1002"));
    }
}
