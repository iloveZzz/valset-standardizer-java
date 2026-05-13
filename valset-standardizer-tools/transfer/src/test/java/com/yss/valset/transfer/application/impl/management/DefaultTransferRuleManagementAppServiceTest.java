package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferRuleUpsertCommand;
import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DefaultTransferRuleManagementAppServiceTest {

    @Test
    void upsertRuleShouldRejectDuplicateRuleCodeBeforeSave() {
        TransferRuleGateway transferRuleGateway = mock(TransferRuleGateway.class);
        DefaultTransferRuleManagementAppService service = new DefaultTransferRuleManagementAppService(transferRuleGateway);

        RuleDefinition existing = new RuleDefinition(
                "100",
                "all-file",
                "已有规则",
                "1.0.0",
                true,
                10,
                "SCRIPT_RULE",
                "qlexpress4",
                "return true;",
                Instant.now(),
                Instant.now(),
                Map.of()
        );
        when(transferRuleGateway.findByRuleCode("all-file")).thenReturn(Optional.of(existing));

        TransferRuleUpsertCommand command = new TransferRuleUpsertCommand();
        command.setRuleCode("all-file");
        command.setRuleName("新规则");

        assertThatThrownBy(() -> service.upsertRule(command))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode().value())
                            .isEqualTo(HttpStatus.CONFLICT.value());
                })
                .hasMessageContaining("规则编码已存在");

        verify(transferRuleGateway).findByRuleCode("all-file");
        verifyNoMoreInteractions(transferRuleGateway);
    }

    @Test
    void upsertRuleShouldTranslateDuplicateConstraintViolation() {
        TransferRuleGateway transferRuleGateway = mock(TransferRuleGateway.class);
        DefaultTransferRuleManagementAppService service = new DefaultTransferRuleManagementAppService(transferRuleGateway);

        when(transferRuleGateway.findByRuleCode("all-file")).thenReturn(Optional.empty());
        when(transferRuleGateway.save(any(RuleDefinition.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'all-file' for key 't_transfer_rule.uk_transfer_rule_code'"));

        TransferRuleUpsertCommand command = new TransferRuleUpsertCommand();
        command.setRuleCode("all-file");
        command.setRuleName("新规则");

        assertThatThrownBy(() -> service.upsertRule(command))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    org.assertj.core.api.Assertions.assertThat(exception.getStatusCode().value())
                            .isEqualTo(HttpStatus.CONFLICT.value());
                })
                .hasMessageContaining("规则编码已存在");

        verify(transferRuleGateway).findByRuleCode("all-file");
    }
}
