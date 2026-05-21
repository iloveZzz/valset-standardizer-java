package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.entity.TrSpvJjhzgzbPO;
import com.yss.valset.extract.repository.mapper.TrSpvJjhzgzbRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrSpvJjhzgzbGatewayImplTest {

    @Mock
    private TrSpvJjhzgzbRepository repository;

    @Mock
    private ProductBusinessFieldResolver productBusinessFieldResolver;

    @Mock
    private DatabaseDialectSupport databaseDialectSupport;

    private TrSpvJjhzgzbGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new TrSpvJjhzgzbGatewayImpl(repository, productBusinessFieldResolver, databaseDialectSupport);
        when(productBusinessFieldResolver.resolve(100L)).thenReturn(new ProductBusinessFields("PD001", "ORG001"));
    }

    @Test
    void shouldDeleteSnapshotByProductOrgAndBizDateBeforeInsert() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        gateway.saveStandardizedJjhzgzb(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .subjects(Arrays.asList(subject("1001", 1), subject("1002", 2)))
                .build());

        verify(repository).delete(any());
        ArgumentCaptor<java.util.List<TrSpvJjhzgzbPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(2);
        assertThat(rowsCaptor.getValue()).extracting(TrSpvJjhzgzbPO::getSubjectCd)
                .containsExactly("1001", "1002");
        assertThat(TrSpvJjhzgzbPO.class.getAnnotation(TableName.class).value())
                .isEqualTo("tr_spv_jjhzgzb");
    }

    @Test
    void shouldAllowSameSubjectCodeWhenSourceRowNumberIsDifferent() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);

        gateway.saveStandardizedJjhzgzb(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .subjects(Arrays.asList(subject("1001", 1), subject("1001", 2)))
                .build());

        verify(repository).delete(any());
        ArgumentCaptor<java.util.List<TrSpvJjhzgzbPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).extracting(TrSpvJjhzgzbPO::getSn)
                .containsExactly(1, 2);
    }

    @Test
    void shouldKeepOriginalSubjectCodeAndNameWhenStandardValuesAreNormalized() {
        when(databaseDialectSupport.isOracle()).thenReturn(false);
        SubjectRecord subject = subject("1001", 1);
        subject.setSubjectName("原始科目");
        Map<String, Object> standardValues = new LinkedHashMap<>();
        standardValues.put("subject_cd", "STD1001");
        standardValues.put("subject_nm", "标准科目");
        subject.setStandardValues(standardValues);

        gateway.saveStandardizedJjhzgzb(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .subjects(Collections.singletonList(subject))
                .build());

        ArgumentCaptor<java.util.List<TrSpvJjhzgzbPO>> rowsCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(repository).insertBatchSomeColumn(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).extracting(TrSpvJjhzgzbPO::getSubjectCd)
                .containsExactly("1001");
        assertThat(rowsCaptor.getValue()).extracting(TrSpvJjhzgzbPO::getSubjectNm)
                .containsExactly("原始科目");
    }

    @Test
    void shouldRejectDuplicateSubjectBusinessKeyWithSameSourceRowNumberBeforeDeleting() {
        assertThatThrownBy(() -> gateway.saveStandardizedJjhzgzb(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .subjects(Arrays.asList(subject("1001", 1), subject("1001", 1)))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复业务键")
                .hasMessageContaining("subjectCd=1001")
                .hasMessageContaining("sn=1");

        verify(repository, never()).delete(any());
        verify(repository, never()).insertBatchSomeColumn(any());
    }

    @Test
    void shouldRejectMissingUniqueKeyBeforeDeleting() {
        when(productBusinessFieldResolver.resolve(100L)).thenReturn(new ProductBusinessFields(null, "ORG001"));

        assertThatThrownBy(() -> gateway.saveStandardizedJjhzgzb(1L, 100L, "EXCEL", "fingerprint", ParsedValuationData.builder()
                .basicInfo(Collections.singletonMap("biz_date", "20240520"))
                .subjects(Collections.singletonList(subject("1001", 1)))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PD_CD");

        verify(repository, never()).delete(any());
        verify(repository, never()).insertBatchSomeColumn(any());
    }

    private SubjectRecord subject(String subjectCd, int rowNumber) {
        return SubjectRecord.builder()
                .rowDataNumber(rowNumber)
                .subjectCode(subjectCd)
                .subjectName("科目" + subjectCd)
                .build();
    }
}
