package com.yss.valset.qlexpress.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionUsageDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionUsageReferenceDTO;
import com.yss.valset.qlexpress.infrastructure.entity.QlexpressFunctionPO;
import com.yss.valset.qlexpress.infrastructure.mapper.QlexpressFunctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultQlexpressFunctionUsageAppServiceTest {

    private QlexpressFunctionRepository functionRepository;
    private DefaultQlexpressFunctionUsageAppService.QlexpressFunctionUsageQueryRepository usageQueryRepository;
    private DefaultQlexpressFunctionUsageAppService service;

    @BeforeEach
    void setUp() {
        functionRepository = mock(QlexpressFunctionRepository.class);
        usageQueryRepository = mock(DefaultQlexpressFunctionUsageAppService.QlexpressFunctionUsageQueryRepository.class);
        service = new DefaultQlexpressFunctionUsageAppService(functionRepository, usageQueryRepository, new ObjectMapper());
    }

    @Test
    void mapsSourceModulesToCoreFlowStages() {
        QlexpressFunctionPO function = function("1", "isHeaderRow", "{\"sourceModules\":[\"extract.parse\"]}", "function isHeaderRow(row) { return true; }");

        QlexpressFunctionUsageDTO usage = service.summarizeUsage(function);

        assertThat(usage.getSourceModules()).containsExactly("extract.parse");
        assertThat(usage.getFlowUsages())
                .filteredOn(item -> Boolean.TRUE.equals(item.getMatched()))
                .extracting("flowCode")
                .containsExactly("FILE_PARSE");
    }

    @Test
    void detectsDirectReferenceFromQlexpressExpression() {
        QlexpressFunctionPO function = function("1", "containsAnyText", "{\"sourceModules\":[\"transfer.rule\"]}", "function containsAnyText(source, keywords) { return true; }");
        when(usageQueryRepository.listTransferRuleSources()).thenReturn(Collections.singletonList(
                new DefaultQlexpressFunctionUsageAppService.ExpressionSource(
                        "TRANSFER_RECOGNITION",
                        "文件接入与识别",
                        "transfer.rule",
                        "containsAnyText(fileName, [\"估值表\"])",
                        "TRANSFER_RULE",
                        "分拣规则",
                        "rule-1",
                        "估值表分拣规则",
                        Boolean.TRUE
                )
        ));

        QlexpressFunctionUsageDTO usage = service.getUsage("1");

        assertThat(usage.getUsageStatus()).isEqualTo(DefaultQlexpressFunctionUsageAppService.STATUS_DIRECT_REFERENCED);
        assertThat(usage.getDirectReferences())
                .extracting(QlexpressFunctionUsageReferenceDTO::getSourceName)
                .contains("估值表分拣规则");
    }

    @Test
    void detectsDependencyReferenceBetweenFunctionScripts() {
        QlexpressFunctionPO target = function("1", "baseFn", "{\"sourceModules\":[\"extract.parse\"]}", "function baseFn(value) { return true; }");
        QlexpressFunctionPO caller = function("2", "callerFn", "{\"sourceModules\":[\"extract.parse\"]}", "function callerFn(value) { return baseFn(value); }");
        when(functionRepository.selectById("1")).thenReturn(target);
        when(functionRepository.selectList(any())).thenReturn(Arrays.asList(target, caller));

        QlexpressFunctionUsageDTO usage = service.getUsage("1");

        assertThat(usage.getUsageStatus()).isEqualTo(DefaultQlexpressFunctionUsageAppService.STATUS_DEPENDENCY_ONLY);
        assertThat(usage.getDependencyReferences())
                .extracting(QlexpressFunctionUsageReferenceDTO::getSourceName)
                .contains("callerFn");
    }

    @Test
    void unusedFunctionRemainsScopeAvailable() {
        QlexpressFunctionPO target = function("1", "unusedFn", "{\"sourceModules\":[\"extract.headerMapping\"]}", "function unusedFn(value) { return true; }");
        when(functionRepository.selectById("1")).thenReturn(target);
        when(functionRepository.selectList(any())).thenReturn(Collections.singletonList(target));

        QlexpressFunctionUsageDTO usage = service.getUsage("1");

        assertThat(usage.getUsageStatus()).isEqualTo(DefaultQlexpressFunctionUsageAppService.STATUS_SCOPE_AVAILABLE);
        assertThat(usage.getUsageStatusName()).isEqualTo("作用域可用但未直接引用");
    }

    private QlexpressFunctionPO function(String id, String name, String extInfoJson, String scriptBody) {
        QlexpressFunctionPO po = new QlexpressFunctionPO();
        po.setFunctionId(id);
        po.setFunctionName(name);
        po.setFunctionCnName(name);
        po.setExtInfoJson(extInfoJson);
        po.setScriptBody(scriptBody);
        po.setEnabled(Boolean.TRUE);
        when(functionRepository.selectById(id)).thenReturn(po);
        when(functionRepository.selectList(any())).thenReturn(Collections.singletonList(po));
        return po;
    }
}
