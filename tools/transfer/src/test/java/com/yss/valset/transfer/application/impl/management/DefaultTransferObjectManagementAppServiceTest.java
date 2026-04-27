package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferObjectRetagCommand;
import com.yss.valset.transfer.application.dto.TransferObjectRetagResponse;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferObjectManagementAppServiceTest {

    @Test
    void shouldRetagAllMatchedObjectsByCurrentFilters() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferProcessUseCase transferProcessUseCase = mock(TransferProcessUseCase.class);
        TransferTaggingUseCase transferTaggingUseCase = mock(TransferTaggingUseCase.class);
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);

        DefaultTransferObjectManagementAppService service = new DefaultTransferObjectManagementAppService(
                transferObjectGateway,
                transferDeliveryGateway,
                transferProcessUseCase,
                transferTaggingUseCase,
                transferTagGateway
        );

        TransferObject first = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "mail-1.xls",
                "xls",
                "application/vnd.ms-excel",
                123L,
                "fingerprint-1",
                "source-ref-1",
                "mail-id-1",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/mail-1.xls",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of()
        );
        TransferObject second = new TransferObject(
                "transfer-2",
                "source-1",
                "EMAIL",
                "source-code",
                "mail-2.xls",
                "xls",
                "application/vnd.ms-excel",
                456L,
                "fingerprint-2",
                "source-ref-2",
                "mail-id-2",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/mail-2.xls",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of()
        );

        when(transferTagGateway.listEnabledTags()).thenReturn(List.of(new TransferTagDefinition(
                "tag-ignored",
                "TAG_CODE",
                "标签",
                "xlsx",
                true,
                10,
                "REGEX_RULE",
                "qlexpress4",
                null,
                "mail-ignored\\.xls",
                Map.of(),
                Instant.now(),
                Instant.now()
        )));
        when(transferObjectGateway.pageObjects(null, null, null, null, null, null, null, null, null, null, null, 0, 200))
                .thenReturn(new TransferObjectPage(List.of(first, second), 2L, 0L, 200L));
        when(transferTaggingUseCase.retag("transfer-1", true))
                .thenReturn(List.of(new TransferObjectTag(
                        null,
                        "transfer-1",
                        "tag-1",
                        "TAG_CODE",
                        "标签",
                        "xlsx",
                        "REGEX_RULE",
                        "命中",
                        "fileName",
                        "mail-1.xls",
                        Map.of(),
                        Instant.now()
                )));
        when(transferTaggingUseCase.retag("transfer-2", true)).thenReturn(List.of());

        TransferObjectRetagResponse response = service.retag(new TransferObjectRetagCommand());

        assertThat(response.getRequestedCount()).isEqualTo(2);
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isEqualTo(0);
        assertThat(response.getMatchedTagCount()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getMessage()).contains("重新打标成功");
        verify(transferTaggingUseCase).retag("transfer-1", true);
        verify(transferTaggingUseCase).retag("transfer-2", true);
    }
}
