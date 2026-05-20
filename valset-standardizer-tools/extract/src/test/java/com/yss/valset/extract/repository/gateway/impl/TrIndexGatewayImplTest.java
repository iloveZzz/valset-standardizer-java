package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.TrIndexPO;
import com.yss.valset.extract.repository.mapper.TrIndexRepository;
import com.yss.valset.extract.support.ProductBusinessFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrIndexGatewayImplTest {

    @Mock
    private TrIndexRepository repository;

    @Mock
    private ProductBusinessFieldResolver productBusinessFieldResolver;

    @Mock
    private DatabaseDialectSupport databaseDialectSupport;

    private TrIndexGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new TrIndexGatewayImpl(repository, productBusinessFieldResolver, databaseDialectSupport);
        when(productBusinessFieldResolver.resolve(100L)).thenReturn(new ProductBusinessFields("PD001", "ORG001"));
    }

    @Test
    void shouldDeleteSnapshotByProductOrgAndBizDateBeforeInsert() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(metric("资产净值", "1"), metric("单位净值", "2")))
                .build());

        verify(repository).delete(any());
        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(2);
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxNm)
                .containsExactly("资产净值", "单位净值");
        assertThat(TrIndexPO.class.getAnnotation(TableName.class).value())
                .isEqualTo("tr_spv_index");
    }

    @Test
    void shouldKeepOriginalMetricNameWhenStandardNameIsNormalized() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        MetricRecord metric = metric("今日单位净值", "1", 10);
        metric.setStandardName("单位净值");
        metric.setStandardValues(new LinkedHashMap<String, Object>() {{
            put("metric_name", "单位净值");
            put("metric_value", "1");
        }});

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Collections.singletonList(metric))
                .build());

        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxNm)
                .containsExactly("今日单位净值");
    }

    @Test
    void shouldAllowSameIndexNameWhenSourceRowNumberIsDifferent() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(metric("资产净值", "1", 10), metric("资产净值", "2", 11)))
                .build());

        verify(repository).delete(any());
        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getSn)
                .containsExactly(10, 11);
    }

    @Test
    void shouldRejectDuplicateIndexBusinessKeyWithSameSourceRowNumberBeforeDeleting() {
        assertThatThrownBy(() -> gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(metric("资产净值", "1", 10), metric("资产净值", "2", 10)))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复业务键")
                .hasMessageContaining("indxNm=资产净值")
                .hasMessageContaining("sn=10");

        verify(repository, never()).delete(any());
        verify(repository, never()).insertBatchSomeColumn(any());
    }

    @Test
    void shouldRejectMissingUniqueKeyBeforeDeleting() {
        when(productBusinessFieldResolver.resolve(100L)).thenReturn(new ProductBusinessFields("PD001", null));

        assertThatThrownBy(() -> gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Collections.singletonList(metric("资产净值", "1")))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ORG_CD");

        verify(repository, never()).delete(any());
        verify(repository, never()).insertBatchSomeColumn(any());
    }

    private MetricRecord metric(String metricName, String value) {
        return metric(metricName, value, 1);
    }

    private MetricRecord metric(String metricName, String value, int rowNumber) {
        return MetricRecord.builder()
                .rowDataNumber(rowNumber)
                .metricName(metricName)
                .value(value)
                .build();
    }
}
