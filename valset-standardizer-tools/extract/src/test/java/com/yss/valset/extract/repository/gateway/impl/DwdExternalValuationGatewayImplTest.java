package com.yss.valset.extract.repository.gateway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.entity.DwdExternalValuationPO;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationBasicInfoRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationHeaderRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationMetricRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DwdExternalValuationGatewayImplTest {

    @Mock
    private DwdExternalValuationRepository valuationRepository;

    @Mock
    private DwdExternalValuationBasicInfoRepository basicInfoRepository;

    @Mock
    private DwdExternalValuationHeaderRepository headerRepository;

    @Mock
    private DwdExternalValuationSubjectRepository subjectRepository;

    @Mock
    private DwdExternalValuationMetricRepository metricRepository;

    @Mock
    private DatabaseDialectSupport databaseDialectSupport;

    private DwdExternalValuationGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new DwdExternalValuationGatewayImpl(
                valuationRepository,
                basicInfoRepository,
                headerRepository,
                subjectRepository,
                metricRepository,
                new ObjectMapper(),
                databaseDialectSupport
        );
    }

    @Test
    void shouldDeleteOldSnapshotByFileIdBeforeInsert() {
        when(valuationRepository.selectList(any())).thenReturn(Arrays.asList(valuation(10L), valuation(11L)));

        gateway.saveDwdExternalValuation(1L, 100L, parsedData());

        InOrder inOrder = inOrder(
                valuationRepository,
                basicInfoRepository,
                headerRepository,
                metricRepository,
                subjectRepository
        );
        inOrder.verify(valuationRepository).selectList(any());
        inOrder.verify(basicInfoRepository).delete(any());
        inOrder.verify(headerRepository).delete(any());
        inOrder.verify(metricRepository).delete(any());
        inOrder.verify(subjectRepository).delete(any());
        inOrder.verify(valuationRepository).delete(any());
        inOrder.verify(valuationRepository).insert(any(DwdExternalValuationPO.class));
    }

    @Test
    void shouldDeleteMainSnapshotWhenNoOldChildValuationExists() {
        when(valuationRepository.selectList(any())).thenReturn(Collections.emptyList());

        gateway.saveDwdExternalValuation(1L, 100L, parsedData());

        verify(basicInfoRepository, never()).delete(any());
        verify(headerRepository, never()).delete(any());
        verify(metricRepository, never()).delete(any());
        verify(subjectRepository, never()).delete(any());
        verify(valuationRepository).delete(any());
        verify(valuationRepository).insert(any(DwdExternalValuationPO.class));
    }

    @Test
    void shouldRejectMissingFileIdBeforeDeleting() {
        assertThatThrownBy(() -> gateway.saveDwdExternalValuation(1L, null, parsedData()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解析文件ID为空");

        verify(valuationRepository, never()).selectList(any());
        verify(valuationRepository, never()).delete(any());
        verify(valuationRepository, never()).insert(any(DwdExternalValuationPO.class));
    }

    private ParsedValuationData parsedData() {
        return ParsedValuationData.builder()
                .workbookPath("/tmp/demo.xlsx")
                .sheetName("估值表")
                .headerRowNumber(1)
                .dataStartRowNumber(2)
                .title("估值表")
                .basicInfo(Collections.singletonMap("业务日期", "20240520"))
                .headers(Arrays.asList("科目代码", "科目名称", "资产净值"))
                .subjects(Collections.singletonList(SubjectRecord.builder()
                        .rowDataNumber(2)
                        .subjectCode("1002")
                        .subjectName("银行存款")
                        .build()))
                .metrics(Collections.singletonList(MetricRecord.builder()
                        .rowDataNumber(3)
                        .metricName("资产净值")
                        .value("100.00")
                        .build()))
                .build();
    }

    private DwdExternalValuationPO valuation(Long id) {
        DwdExternalValuationPO valuationPO = new DwdExternalValuationPO();
        valuationPO.setId(id);
        valuationPO.setFileId(100L);
        return valuationPO;
    }
}
