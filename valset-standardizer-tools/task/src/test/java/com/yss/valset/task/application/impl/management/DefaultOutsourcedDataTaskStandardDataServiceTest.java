package com.yss.valset.task.application.impl.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.extract.repository.entity.DwdExternalValuationBasicInfoPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationHeaderPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationMetricPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationPO;
import com.yss.valset.extract.repository.entity.DwdExternalValuationSubjectPO;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationBasicInfoRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationHeaderRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationMetricRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationRepository;
import com.yss.valset.extract.repository.mapper.DwdExternalValuationSubjectRepository;
import com.yss.valset.task.application.command.OutsourcedDataTaskStandardDataExportCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardDataExportDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardBasicDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardMetricDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardSubjectDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.application.support.UniverWorkbookExportSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultOutsourcedDataTaskStandardDataServiceTest {

    private OutsourcedDataTaskGateway taskGateway;
    private DwdExternalValuationRepository valuationRepository;
    private DwdExternalValuationBasicInfoRepository basicInfoRepository;
    private DwdExternalValuationHeaderRepository headerRepository;
    private DwdExternalValuationSubjectRepository subjectRepository;
    private DwdExternalValuationMetricRepository metricRepository;
    private ObjectMapper objectMapper;
    private UniverWorkbookExportSupport exportSupport;
    private DefaultOutsourcedDataTaskStandardDataService service;

    @BeforeEach
    void setUp() {
        taskGateway = mock(OutsourcedDataTaskGateway.class);
        valuationRepository = mock(DwdExternalValuationRepository.class);
        basicInfoRepository = mock(DwdExternalValuationBasicInfoRepository.class);
        headerRepository = mock(DwdExternalValuationHeaderRepository.class);
        subjectRepository = mock(DwdExternalValuationSubjectRepository.class);
        metricRepository = mock(DwdExternalValuationMetricRepository.class);
        objectMapper = new ObjectMapper();
        exportSupport = mock(UniverWorkbookExportSupport.class);
        DatabaseDialectSupport databaseDialectSupport = mock(DatabaseDialectSupport.class);
        when(databaseDialectSupport.limitClause(1)).thenReturn("limit 1");
        service = new DefaultOutsourcedDataTaskStandardDataService(
                taskGateway,
                valuationRepository,
                basicInfoRepository,
                headerRepository,
                subjectRepository,
                metricRepository,
                databaseDialectSupport,
                objectMapper,
                exportSupport
        );
    }

    @Test
    void queryBasicUsesFileIdBeforeTaskId() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(valuation(10L, 100L, 900L));
        DwdExternalValuationBasicInfoPO productName = new DwdExternalValuationBasicInfoPO();
        productName.setInfoKey("产品名称");
        productName.setInfoValue("测试产品");
        when(basicInfoRepository.selectList(any())).thenReturn(Collections.singletonList(productName));
        when(headerRepository.selectList(any())).thenReturn(Arrays.asList(
                header(2, "市值"),
                header(0, "科目编码"),
                header(1, "科目名称"),
                header(3, "成本")
        ));
        when(subjectRepository.selectCount(any())).thenReturn(2L);
        when(metricRepository.selectCount(any())).thenReturn(3L);

        OutsourcedDataTaskStandardBasicDTO result = service.queryBasic("FILE-100");

        assertThat(result.getValuationId()).isEqualTo(10L);
        assertThat(result.getFileId()).isEqualTo(100L);
        assertThat(result.getTaskId()).isEqualTo(900L);
        assertThat(result.getBasicInfoCount()).isEqualTo(1L);
        assertThat(result.getSubjectCount()).isEqualTo(2L);
        assertThat(result.getMetricCount()).isEqualTo(3L);
        assertThat(result.getBasicRows())
                .extracting("category", "fieldName", "fieldValue")
                .contains(
                        org.assertj.core.groups.Tuple.tuple("主表信息", "估值ID", "10"),
                        org.assertj.core.groups.Tuple.tuple("基础信息", "产品名称", "测试产品")
                );
        assertThat(result.getRawColumns())
                .extracting("fieldKey", "title", "columnIndex")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("raw_2", "市值", 2),
                        org.assertj.core.groups.Tuple.tuple("raw_3", "成本", 3)
                );
        verify(valuationRepository, times(1)).selectOne(any());
    }

    @Test
    void queryBasicFallsBackToTaskIdWhenFileIdMissing() {
        when(taskGateway.findTask("TASK-900")).thenReturn(Optional.of(batch("TASK-900", null, 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(valuation(11L, null, 900L));
        when(basicInfoRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(subjectRepository.selectCount(any())).thenReturn(0L);
        when(metricRepository.selectCount(any())).thenReturn(0L);

        OutsourcedDataTaskStandardBasicDTO result = service.queryBasic("TASK-900");

        assertThat(result.getValuationId()).isEqualTo(11L);
        assertThat(result.getTaskId()).isEqualTo(900L);
        verify(valuationRepository, times(1)).selectOne(any());
    }

    @Test
    void queryBasicFailsWhenBatchMissing() {
        when(taskGateway.findTask("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.queryBasic("MISSING"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("未找到批次对应的估值解析任务");
        verify(valuationRepository, never()).selectOne(any());
    }

    @Test
    void queryBasicReturnsEmptyWhenValuationMissing() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(null);

        OutsourcedDataTaskStandardBasicDTO result = service.queryBasic("FILE-100");

        assertThat(result.getBatchId()).isEqualTo("FILE-100");
        assertThat(result.getFileId()).isEqualTo(100L);
        assertThat(result.getTaskId()).isEqualTo(900L);
        assertThat(result.getBasicInfoCount()).isZero();
        assertThat(result.getSubjectCount()).isZero();
        assertThat(result.getMetricCount()).isZero();
        assertThat(result.getBasicRows()).isEmpty();
    }

    @Test
    void queryBasicFiltersFirstTwoHeaderColumnsFromRawColumns() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(valuation(10L, 100L, 900L));
        when(basicInfoRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(subjectRepository.selectCount(any())).thenReturn(0L);
        when(metricRepository.selectCount(any())).thenReturn(0L);
        when(headerRepository.selectList(any())).thenReturn(Arrays.asList(
                header(0, "科目编码"),
                header(1, "科目名称"),
                header(2, "市值"),
                header(3, "成本")
        ));

        OutsourcedDataTaskStandardBasicDTO result = service.queryBasic("FILE-100");

        assertThat(result.getRawColumns())
                .extracting("fieldKey", "title", "columnIndex")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("raw_2", "市值", 2),
                        org.assertj.core.groups.Tuple.tuple("raw_3", "成本", 3)
                );
    }

    @Test
    void listSubjectsReturnsAllRows() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(valuation(10L, 100L, 900L));
        DwdExternalValuationSubjectPO po = new DwdExternalValuationSubjectPO();
        po.setId(1L);
        po.setValuationId(10L);
        po.setSubjectCode("1101");
        po.setSubjectName("银行存款");
        po.setRowDataNumber(5);
        po.setRawValuesJson("[\"1101\",\"银行存款\",\"100.00\",\"90.00\"]");
        when(subjectRepository.selectList(any())).thenReturn(Collections.singletonList(po));
        when(headerRepository.selectList(any())).thenReturn(Arrays.asList(
                header(0, "科目编码"),
                header(1, "科目名称"),
                header(2, "市值"),
                header(3, "成本")
        ));

        List<OutsourcedDataTaskStandardSubjectDTO> result = service.listSubjects("FILE-100", "银行");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubjectCode()).isEqualTo("1101");
        assertThat(result.get(0).getRawValues())
                .containsEntry("raw_2", "100.00")
                .containsEntry("raw_3", "90.00")
                .doesNotContainKeys("raw_0", "raw_1");
    }

    @Test
    void listMetricsReturnsAllRows() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(valuation(10L, 100L, 900L));
        DwdExternalValuationMetricPO po = new DwdExternalValuationMetricPO();
        po.setId(2L);
        po.setValuationId(10L);
        po.setMetricName("单位净值");
        po.setMetricType("NUMBER");
        po.setMetricValue("1.0000");
        po.setRawValuesJson("{\"科目名称\":\"单位净值\",\"指标值\":\"1.0000\",\"备注\":\"复核\"}");
        when(metricRepository.selectList(any())).thenReturn(Arrays.asList(po));
        when(headerRepository.selectList(any())).thenReturn(Collections.emptyList());

        List<OutsourcedDataTaskStandardMetricDTO> result = service.listMetrics("FILE-100", "净值");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetricName()).isEqualTo("单位净值");
        assertThat(result.get(0).getRawValues())
                .containsEntry("指标值", "1.0000")
                .containsEntry("备注", "复核")
                .doesNotContainKey("科目名称");
    }

    @Test
    void listMetricsIgnoresInvalidRawValuesJson() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(valuation(10L, 100L, 900L));
        DwdExternalValuationMetricPO po = new DwdExternalValuationMetricPO();
        po.setId(3L);
        po.setValuationId(10L);
        po.setMetricName("错误JSON");
        po.setRawValuesJson("{invalid");
        when(metricRepository.selectList(any())).thenReturn(Collections.singletonList(po));
        when(headerRepository.selectList(any())).thenReturn(Collections.singletonList(header(2, "市值")));

        List<OutsourcedDataTaskStandardMetricDTO> result = service.listMetrics("FILE-100", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRawValues()).isEmpty();
    }

    @Test
    void listSubjectsReturnsEmptyWhenValuationMissing() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));
        when(valuationRepository.selectOne(any())).thenReturn(null);

        List<OutsourcedDataTaskStandardSubjectDTO> result = service.listSubjects("FILE-100", "银行");

        assertThat(result).isEmpty();
        verify(subjectRepository, never()).selectList(any());
    }

    @Test
    void exportSheetRequiresExistingBatch() throws Exception {
        when(taskGateway.findTask("MISSING")).thenReturn(Optional.empty());
        OutsourcedDataTaskStandardDataExportCommand command = new OutsourcedDataTaskStandardDataExportCommand();
        command.setWorkbookData(objectMapper.readTree("{\"sheets\":{\"sheet1\":{}}}"));

        assertThatThrownBy(() -> service.exportSheet("MISSING", command))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("未找到批次对应的估值解析任务");
    }

    @Test
    void exportSheetRejectsEmptyWorkbookData() {
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch("FILE-100", "100", 900L)));

        assertThatThrownBy(() -> service.exportSheet("FILE-100", new OutsourcedDataTaskStandardDataExportCommand()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("导出工作簿快照不能为空");
    }

    @Test
    void exportSheetBuildsFileNameAndDelegatesSnapshotExport() throws Exception {
        OutsourcedDataTaskBatchDTO batch = batch("FILE-100", "100", 900L);
        batch.setBatchName("批次/一");
        when(taskGateway.findTask("FILE-100")).thenReturn(Optional.of(batch));
        when(exportSupport.export(any(), any())).thenReturn(new byte[] {1, 2, 3});

        OutsourcedDataTaskStandardDataExportCommand command = new OutsourcedDataTaskStandardDataExportCommand();
        command.setTab("subjects");
        command.setSheetName("估值明细");
        command.setWorkbookData(objectMapper.readTree("{\"sheets\":{\"sheet1\":{}}}"));

        OutsourcedDataTaskStandardDataExportDTO result = service.exportSheet("FILE-100", command);

        assertThat(result.getFileName()).isEqualTo("估值标准数据_批次_一_估值明细.xlsx");
        assertThat(result.getContent()).containsExactly(1, 2, 3);
    }

    private OutsourcedDataTaskBatchDTO batch(String batchId, String fileId, Long taskId) {
        OutsourcedDataTaskBatchDTO batch = new OutsourcedDataTaskBatchDTO();
        batch.setBatchId(batchId);
        batch.setFileId(fileId);
        batch.setTaskId(taskId);
        return batch;
    }

    private DwdExternalValuationPO valuation(Long valuationId, Long fileId, Long taskId) {
        DwdExternalValuationPO valuation = new DwdExternalValuationPO();
        valuation.setId(valuationId);
        valuation.setFileId(fileId);
        valuation.setTaskId(taskId);
        valuation.setWorkbookPath("/tmp/demo.xlsx");
        valuation.setSheetName("估值表");
        valuation.setTitle("测试估值表");
        valuation.setHeaderRowNumber(3);
        valuation.setDataStartRowNumber(4);
        return valuation;
    }

    private DwdExternalValuationHeaderPO header(Integer columnIndex, String headerName) {
        DwdExternalValuationHeaderPO header = new DwdExternalValuationHeaderPO();
        header.setId(columnIndex == null ? null : columnIndex.longValue());
        header.setValuationId(10L);
        header.setColumnIndex(columnIndex);
        header.setHeaderName(headerName);
        return header;
    }
}
