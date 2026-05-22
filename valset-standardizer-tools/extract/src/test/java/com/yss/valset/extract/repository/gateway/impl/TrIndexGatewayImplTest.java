package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.entity.TcAsIndexPO;
import com.yss.valset.extract.repository.entity.TrIndexPO;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import com.yss.valset.extract.repository.mapper.TcAsIndexRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrIndexGatewayImplTest {

    @Mock
    private TrIndexRepository repository;

    @Mock
    private FileParseSourceRepository fileParseSourceRepository;

    @Mock
    private TcAsIndexRepository tcAsIndexRepository;

    @Mock
    private ProductBusinessFieldResolver productBusinessFieldResolver;

    @Mock
    private DatabaseDialectSupport databaseDialectSupport;

    private TrIndexGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new TrIndexGatewayImpl(repository, fileParseSourceRepository, tcAsIndexRepository, productBusinessFieldResolver, databaseDialectSupport);
        when(productBusinessFieldResolver.resolve(100L)).thenReturn(new ProductBusinessFields("PD001", "ORG001"));
        lenient().when(fileParseSourceRepository.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldDeleteSnapshotByProductOrgAndBizDateBeforeInsert() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(metricData("资产净值", "1"), metricData("单位净值", "2")))
                .build());

        verify(repository).delete(any());
        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(2);
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxNm)
                .containsExactly("资产净值", "单位净值");
        assertThat(TrIndexPO.class.getAnnotation(TableName.class).value())
                .isEqualTo("tr_spv_index");
        verify(tcAsIndexRepository).delete(any());
    }

    @Test
    void shouldKeepOriginalMetricNameWhenStandardNameIsNormalized() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        MetricRecord metric = metricData("今日单位净值", "1", 10);
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
                .metrics(Arrays.asList(metricData("资产净值", "1", 10), metricData("资产净值", "2", 11)))
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
                .metrics(Arrays.asList(metricData("资产净值", "1", 10), metricData("资产净值", "2", 10)))
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
                .metrics(Collections.singletonList(metricData("资产净值", "1")))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ORG_CD");

        verify(repository, never()).delete(any());
        verify(repository, never()).insertBatchSomeColumn(any());
    }

    @Test
    void shouldSaveMetricDataRowsWhenValueIsNumeric() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "valuation_20240520.xlsx", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(
                        metricData("偏离金额", "-19852.2", 20),
                        metricData("净值(成本)", "8,630,309,139.82", 21),
                        metricData("非数字指标", "说明文本", 22)))
                .build());

        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(2);
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxNm)
                .containsExactly("偏离金额", "净值(成本)");
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxValu)
                .containsExactly("-19852.2", "8630309139.82");
    }

    @Test
    void shouldExpandMetricRowNumericValuesIntoIndexRows() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        MetricRecord metric = MetricRecord.builder()
                .rowDataNumber(30)
                .metricName("今日可用头寸")
                .metricType("metric_row")
                .rawValues(new LinkedHashMap<String, Object>() {{
                    put("成本|本币|十亿千百十万千百十元角分", "6,037,207,981.64");
                    put("成本占比", "69.95%");
                    put("市值|本币|十亿千百十万千百十元角分", "6037207981.64");
                    put("行情", "停牌");
                    put("wind代码", "ABC001.SH");
                    put("空列", "");
                }})
                .build();

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "valuation_20240520.xlsx", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Collections.singletonList(metric))
                .build());

        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(3);
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxNm)
                .containsExactly(
                        "今日可用头寸|成本|本币|十亿千百十万千百十元角分",
                        "今日可用头寸|成本占比",
                        "今日可用头寸|市值|本币|十亿千百十万千百十元角分");
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxValu)
                .containsExactly("6037207981.64", "0.6995", "6037207981.64");
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getSn)
                .containsExactly(30, 30, 30);
    }

    @Test
    void shouldDeduplicateMetricRowHeaderPathSegmentsBeforeBuildingIndexName() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        MetricRecord metric = MetricRecord.builder()
                .rowDataNumber(40)
                .metricName("实收资本金额")
                .metricType("metric_row")
                .rawValues(new LinkedHashMap<String, Object>() {{
                    put("数量|数量|数量", "100");
                    put("市值占比|市值占比|市值占比", "12.50%");
                    put("数量", "200");
                }})
                .build();

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "valuation_20240520.xlsx", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Collections.singletonList(metric))
                .build());

        ArgumentCaptor<java.util.List<TrIndexPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(2);
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxNm)
                .containsExactly("实收资本金额|数量", "实收资本金额|市值占比");
        assertThat(rowsCaptor.getValue()).extracting(TrIndexPO::getIndxValu)
                .containsExactly("100", "0.125");
    }

    @Test
    void shouldBuildTcAsIndexWideRowsFromMappedIndexNames() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);
        when(fileParseSourceRepository.selectList(any())).thenReturn(Arrays.asList(
                parseSource("资产净值", "asset_value"),
                parseSource("今日单位净值", "avg_nav")));

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(metricData("资产净值", "12.34"), metricData("今日单位净值", "1.2345")))
                .build());

        ArgumentCaptor<java.util.List<TcAsIndexPO>> wideRowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(tcAsIndexRepository).delete(any());
        verify(tcAsIndexRepository).insertBatchSomeColumn(wideRowsCaptor.capture());
        assertThat(wideRowsCaptor.getValue()).hasSize(1);
        TcAsIndexPO wideRow = wideRowsCaptor.getValue().get(0);
        assertThat(wideRow.getPdCd()).isEqualTo("PD001");
        assertThat(wideRow.getOrgCd()).isEqualTo("ORG001");
        assertThat(wideRow.getBizDate()).isEqualTo("20240520");
        assertThat(wideRow.getAssetValue()).isEqualByComparingTo("12.34");
        assertThat(wideRow.getAvgNav()).isEqualByComparingTo("1.2345");
        assertThat(wideRow.getIndexType()).isNull();
        assertThat(wideRow.getId()).isNotNull();
    }

    @Test
    void shouldSkipUnmappedAndNonNumericIndexValuesWhenBuildingTcAsIndex() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);
        when(fileParseSourceRepository.selectList(any())).thenReturn(Collections.singletonList(parseSource("资产净值", "asset_value")));

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(
                        metricData("资产净值", "说明文本", 10),
                        metricData("未映射指标", "9", 11)))
                .build());

        verify(tcAsIndexRepository).delete(any());
        verify(tcAsIndexRepository, never()).insertBatchSomeColumn(any());
    }

    @Test
    void shouldUseLastMappedValueWhenMultipleIndexNamesMapToSameTcAsIndexField() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);
        when(fileParseSourceRepository.selectList(any())).thenReturn(Arrays.asList(
                parseSource("资产净值", "asset_value"),
                parseSource("净值(市值)", "asset_value")));

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Arrays.asList(
                        metricData("资产净值", "1", 10),
                        metricData("净值(市值)", "2", 11)))
                .build());

        ArgumentCaptor<java.util.List<TcAsIndexPO>> wideRowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(tcAsIndexRepository).insertBatchSomeColumn(wideRowsCaptor.capture());
        assertThat(wideRowsCaptor.getValue()).hasSize(1);
        assertThat(wideRowsCaptor.getValue().get(0).getAssetValue()).isEqualByComparingTo("2");
    }

    @Test
    void shouldInsertTcAsIndexRowsOneByOneWhenOracle() {
        when(databaseDialectSupport.isOracle()).thenReturn(true);
        when(fileParseSourceRepository.selectList(any())).thenReturn(Collections.singletonList(parseSource("资产净值", "asset_value")));

        gateway.saveStandardizedIndex(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .metrics(Collections.singletonList(metricData("资产净值", "1")))
                .build());

        verify(tcAsIndexRepository).insert(any(TcAsIndexPO.class));
        verify(tcAsIndexRepository, never()).insertBatchSomeColumn(any());
    }

    private MetricRecord metricData(String metricName, String value) {
        return metricData(metricName, value, 1);
    }

    private MetricRecord metricData(String metricName, String value, int rowNumber) {
        return MetricRecord.builder()
                .rowDataNumber(rowNumber)
                .metricName(metricName)
                .metricType("metric_data")
                .value(value)
                .build();
    }

    private FileParseSourcePO parseSource(String columnName, String columnMap) {
        FileParseSourcePO po = new FileParseSourcePO();
        po.setColumnName(columnName);
        po.setColumnMap(columnMap);
        po.setStatus(Boolean.TRUE);
        return po;
    }
}
