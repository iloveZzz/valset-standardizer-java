package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferTagUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTagMutationResponse;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferTagManagementAppServiceTest {

    @Test
    void shouldForceDefaultTagEnabledWhenCreating() {
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);
        TransferTaggingUseCase transferTaggingUseCase = mock(TransferTaggingUseCase.class);
        DefaultTransferTagManagementAppService service = new DefaultTransferTagManagementAppService(
                transferTagGateway,
                transferTaggingUseCase
        );

        when(transferTagGateway.findByTagCode("BUSINESS_DATE")).thenReturn(Optional.empty());
        when(transferTagGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferTagUpsertCommand command = new TransferTagUpsertCommand();
        command.setTagCode("BUSINESS_DATE");
        command.setTagName("业务日期");
        command.setTagValue("DATE");
        command.setEnabled(Boolean.FALSE);
        command.setMatchStrategy("REGEX_RULE");
        command.setRegexPattern("\\d{8}");
        command.setTagMeta(Map.of("defaultTag", true));

        TransferTagMutationResponse response = service.upsertTag(command);

        ArgumentCaptor<TransferTagDefinition> captor = ArgumentCaptor.forClass(TransferTagDefinition.class);
        verify(transferTagGateway).save(captor.capture());
        assertThat(captor.getValue().enabled()).isTrue();
        assertThat(response.getTag()).isNotNull();
        assertThat(response.getTag().getEnabled()).isTrue();
        assertThat(response.getTag().getDefaultTag()).isTrue();
    }

    @Test
    void shouldRejectEditingDefaultTag() {
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);
        TransferTaggingUseCase transferTaggingUseCase = mock(TransferTaggingUseCase.class);
        DefaultTransferTagManagementAppService service = new DefaultTransferTagManagementAppService(
                transferTagGateway,
                transferTaggingUseCase
        );

        when(transferTagGateway.findByTagCode("BUSINESS_DATE")).thenReturn(Optional.empty());
        when(transferTagGateway.findById("tag-default")).thenReturn(Optional.of(defaultTagDefinition()));

        TransferTagUpsertCommand command = new TransferTagUpsertCommand();
        command.setTagId("tag-default");
        command.setTagCode("BUSINESS_DATE");
        command.setTagName("业务日期");
        command.setTagValue("DATE");
        command.setMatchStrategy("REGEX_RULE");
        command.setRegexPattern("\\d{8}");

        assertThatThrownBy(() -> service.upsertTag(command))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("默认标签不可编辑");
    }

    @Test
    void shouldRejectMarkingNonBusinessTagAsDefault() {
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);
        TransferTaggingUseCase transferTaggingUseCase = mock(TransferTaggingUseCase.class);
        DefaultTransferTagManagementAppService service = new DefaultTransferTagManagementAppService(
                transferTagGateway,
                transferTaggingUseCase
        );

        when(transferTagGateway.findByTagCode("OTHER_TAG")).thenReturn(Optional.empty());

        TransferTagUpsertCommand command = new TransferTagUpsertCommand();
        command.setTagCode("OTHER_TAG");
        command.setTagName("其他标签");
        command.setTagValue("DATE");
        command.setMatchStrategy("REGEX_RULE");
        command.setRegexPattern("\\d{8}");
        command.setDefaultTag(Boolean.TRUE);

        assertThatThrownBy(() -> service.upsertTag(command))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("默认标签仅允许业务日期标签");
    }

    @Test
    void shouldRejectDefaultMetaOnNonBusinessTagEvenWithoutFlag() {
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);
        TransferTaggingUseCase transferTaggingUseCase = mock(TransferTaggingUseCase.class);
        DefaultTransferTagManagementAppService service = new DefaultTransferTagManagementAppService(
                transferTagGateway,
                transferTaggingUseCase
        );

        when(transferTagGateway.findByTagCode("OTHER_TAG")).thenReturn(Optional.empty());

        TransferTagUpsertCommand command = new TransferTagUpsertCommand();
        command.setTagCode("OTHER_TAG");
        command.setTagName("其他标签");
        command.setTagValue("DATE");
        command.setMatchStrategy("REGEX_RULE");
        command.setRegexPattern("\\d{8}");
        command.setTagMeta(Map.of("defaultTag", true));

        assertThatThrownBy(() -> service.upsertTag(command))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("默认标签仅允许业务日期标签");
    }

    @Test
    void shouldRejectDeletingDefaultTag() {
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);
        TransferTaggingUseCase transferTaggingUseCase = mock(TransferTaggingUseCase.class);
        DefaultTransferTagManagementAppService service = new DefaultTransferTagManagementAppService(
                transferTagGateway,
                transferTaggingUseCase
        );

        when(transferTagGateway.findById("tag-default")).thenReturn(Optional.of(defaultTagDefinition()));

        assertThatThrownBy(() -> service.deleteTag("tag-default"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("默认标签不可删除");
    }

    private TransferTagDefinition defaultTagDefinition() {
        return new TransferTagDefinition(
                "tag-default",
                "BUSINESS_DATE",
                "业务日期",
                "DATE",
                true,
                10,
                "REGEX_RULE",
                "qlexpress4",
                null,
                "\\d{8}",
                Map.of("defaultTag", true),
                Instant.now(),
                Instant.now()
        );
    }
}
