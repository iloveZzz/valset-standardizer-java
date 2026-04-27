package com.yss.valset.transfer.infrastructure.gateway;

import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.infrastructure.convertor.TransferRuleMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferRulePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferRuleGatewayImplTest {

    @Test
    void shouldAlwaysPersistScriptBodyWhenSavingRule() {
        TransferRuleRepository repository = mock(TransferRuleRepository.class);
        TransferRuleMapper mapper = mock(TransferRuleMapper.class);

        TransferRulePO po = new TransferRulePO();
        po.setRuleId("2047251487037984770");
        po.setRuleCode("RULE-001");
        po.setRuleName("规则一");
        po.setRuleVersion("1.0.0");
        po.setEnabled(Boolean.TRUE);
        po.setPriority(10);
        po.setMatchStrategy("SCRIPT_RULE");
        po.setScriptLanguage("qlexpress4");
        po.setScriptBody(null);
        po.setEffectiveFrom(null);
        po.setEffectiveTo(null);
        po.setRuleMetaJson("{}");

        RuleDefinition definition = new RuleDefinition(
                "2047251487037984770",
                "RULE-001",
                "规则一",
                "1.0.0",
                true,
                10,
                "SCRIPT_RULE",
                "qlexpress4",
                "fn.matchesRegex(fileName, '.*估值.*\\\\.xlsx?')",
                (Instant) null,
                (Instant) null,
                Map.of()
        );

        when(mapper.toPO(definition)).thenReturn(po);
        when(mapper.toDomain(po)).thenReturn(definition);

        TransferRuleGatewayImpl gateway = new TransferRuleGatewayImpl(repository, mapper);
        gateway.save(definition);

        ArgumentCaptor<TransferRulePO> poCaptor = ArgumentCaptor.forClass(TransferRulePO.class);
        verify(repository).updateById(poCaptor.capture());
        assertThat(poCaptor.getValue().getScriptBody())
                .isEqualTo("fn.matchesRegex(fileName, '.*估值.*\\\\.xlsx?')");
    }
}
