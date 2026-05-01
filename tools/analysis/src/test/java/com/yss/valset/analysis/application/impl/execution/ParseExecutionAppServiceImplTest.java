package com.yss.valset.analysis.application.impl.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.gateway.DwdExternalValuationGateway;
import com.yss.valset.domain.gateway.DwdJjhzgzbGateway;
import com.yss.valset.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.gateway.TrIndexGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.domain.parser.ValuationDataParserProvider;
import com.yss.valset.extract.standardization.ExternalValuationStandardizationService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParseExecutionAppServiceImplTest {

    @Mock
    private WorkflowTaskGateway taskGateway;
    @Mock
    private ValuationDataParserProvider parserProvider;
    @Mock
    private DwdExternalValuationGateway dwdExternalValuationGateway;
    @Mock
    private StandardizedExternalValuationGateway standardizedExternalValuationGateway;
    @Mock
    private DwdJjhzgzbGateway dwdJjhzgzbGateway;
    @Mock
    private TrIndexGateway trIndexGateway;
    @Mock
    private ValsetFileInfoGateway valsetFileInfoGateway;
    @Mock
    private ExternalValuationStandardizationService standardizationService;
    @Mock
    private Tracer tracer;
    @Mock
    private Span rootSpan;
    @Mock
    private Span childSpan;
    @Mock
    private Tracer.SpanInScope spanInScope;

    @Test
    void execute_should_prefer_local_temp_path_when_available() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ParseExecutionAppServiceImpl service = new ParseExecutionAppServiceImpl(
                taskGateway,
                parserProvider,
                dwdExternalValuationGateway,
                standardizedExternalValuationGateway,
                dwdJjhzgzbGateway,
                trIndexGateway,
                valsetFileInfoGateway,
                standardizationService,
                objectMapper,
                tracer,
                null
        );

        Path localTempFile = Files.createTempFile("valset-parse-local", ".xlsx");
        Path realStorageFile = Files.createTempFile("valset-parse-real", ".xlsx");

        WorkflowTask workflowTask = WorkflowTask.builder()
                .taskId(99L)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(TaskStatus.PENDING)
                .taskStage(TaskStage.PARSE)
                .businessKey("PARSE:EXCEL:12")
                .fileId(12L)
                .inputPayload(objectMapper.writeValueAsString(java.util.Map.of(
                        "workbookPath", "/tmp/source.xlsx",
                        "dataSourceType", DataSourceType.EXCEL.name(),
                        "fileId", 12L,
                        "createdBy", "tester"
                )))
                .taskStartTime(LocalDateTime.now())
                .build();

        ValsetFileInfo fileInfo = ValsetFileInfo.builder()
                .fileId(12L)
                .fileNameOriginal("source.xlsx")
                .localTempPath(localTempFile.toString())
                .realStoragePath(realStorageFile.toString())
                .build();

        AtomicReference<String> selectedSourceUri = new AtomicReference<>();
        ParsedValuationData parsed = ParsedValuationData.builder()
                .sheetName("Sheet1")
                .headerRowNumber(4)
                .dataStartRowNumber(5)
                .subjects(List.of(SubjectRecord.builder().subjectCode("1001").subjectName("现金").build()))
                .metrics(List.of())
                .build();

        when(tracer.nextSpan()).thenReturn(rootSpan, childSpan, childSpan, childSpan, childSpan);
        when(rootSpan.name(any())).thenReturn(rootSpan);
        when(rootSpan.tag(any(), any())).thenReturn(rootSpan);
        when(rootSpan.start()).thenReturn(rootSpan);
        when(childSpan.name(any())).thenReturn(childSpan);
        when(childSpan.start()).thenReturn(childSpan);
        when(tracer.withSpan(any())).thenReturn(spanInScope);
        doNothing().when(rootSpan).end();
        doNothing().when(childSpan).end();

        when(taskGateway.findById(99L)).thenReturn(workflowTask);
        when(valsetFileInfoGateway.findById(12L)).thenReturn(fileInfo);
        ValuationDataParser parser = config -> {
            selectedSourceUri.set(config.getSourceUri());
            return parsed.toBuilder()
                    .workbookPath(config.getSourceUri())
                    .build();
        };
        when(parserProvider.getParser(DataSourceType.EXCEL)).thenReturn(parser);
        when(standardizationService.standardize(any())).thenAnswer(invocation ->
                ((ParsedValuationData) invocation.getArgument(0)).toBuilder()
                        .fileNameOriginal("source.xlsx")
                        .build());
        service.execute(99L);

        verify(taskGateway).findById(99L);
        verify(parserProvider).getParser(DataSourceType.EXCEL);
        verify(dwdExternalValuationGateway).saveDwdExternalValuation(eq(99L), eq(12L), any());
        verify(standardizationService).standardize(any());
        verify(standardizedExternalValuationGateway).saveStandardizedExternalValuation(eq(99L), eq(12L), any());
        verify(dwdJjhzgzbGateway).saveStandardizedJjhzgzb(eq(99L), eq(12L), eq("EXCEL"), eq("source.xlsx"), any());
        verify(trIndexGateway).saveStandardizedIndex(eq(99L), eq(12L), eq("EXCEL"), eq("source.xlsx"), any());
        verify(taskGateway).markSuccess(eq(99L), any());
        verify(taskGateway, never()).markFailed(anyLong(), any());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskGateway).markSuccess(eq(99L), payloadCaptor.capture());
        assertThat(selectedSourceUri.get()).isEqualTo(localTempFile.toString());
        assertThat(payloadCaptor.getValue()).contains("\"workbookPath\":\"" + localTempFile + "\"");
        assertThat(payloadCaptor.getValue()).contains("\"subjectCount\":1");
    }

    @Test
    void execute_should_fallback_to_real_storage_path_when_local_temp_path_missing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ParseExecutionAppServiceImpl service = new ParseExecutionAppServiceImpl(
                taskGateway,
                parserProvider,
                dwdExternalValuationGateway,
                standardizedExternalValuationGateway,
                dwdJjhzgzbGateway,
                trIndexGateway,
                valsetFileInfoGateway,
                standardizationService,
                objectMapper,
                tracer,
                null
        );

        Path realStorageFile = Files.createTempFile("valset-parse-real", ".xlsx");

        WorkflowTask workflowTask = WorkflowTask.builder()
                .taskId(99L)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(TaskStatus.PENDING)
                .taskStage(TaskStage.PARSE)
                .businessKey("PARSE:EXCEL:12")
                .fileId(12L)
                .inputPayload(objectMapper.writeValueAsString(java.util.Map.of(
                        "workbookPath", "/tmp/source.xlsx",
                        "dataSourceType", DataSourceType.EXCEL.name(),
                        "fileId", 12L,
                        "createdBy", "tester"
                )))
                .taskStartTime(LocalDateTime.now())
                .build();

        ValsetFileInfo fileInfo = ValsetFileInfo.builder()
                .fileId(12L)
                .fileNameOriginal("source.xlsx")
                .localTempPath("/tmp/valset-parse-missing-" + System.nanoTime() + ".xlsx")
                .realStoragePath(realStorageFile.toString())
                .build();

        AtomicReference<String> selectedSourceUri = new AtomicReference<>();
        ParsedValuationData parsed = ParsedValuationData.builder()
                .sheetName("Sheet1")
                .headerRowNumber(4)
                .dataStartRowNumber(5)
                .subjects(List.of(SubjectRecord.builder().subjectCode("1001").subjectName("现金").build()))
                .metrics(List.of())
                .build();

        when(tracer.nextSpan()).thenReturn(rootSpan, childSpan, childSpan, childSpan, childSpan);
        when(rootSpan.name(any())).thenReturn(rootSpan);
        when(rootSpan.tag(any(), any())).thenReturn(rootSpan);
        when(rootSpan.start()).thenReturn(rootSpan);
        when(childSpan.name(any())).thenReturn(childSpan);
        when(childSpan.start()).thenReturn(childSpan);
        when(tracer.withSpan(any())).thenReturn(spanInScope);
        doNothing().when(rootSpan).end();
        doNothing().when(childSpan).end();

        when(taskGateway.findById(99L)).thenReturn(workflowTask);
        when(valsetFileInfoGateway.findById(12L)).thenReturn(fileInfo);
        ValuationDataParser parser = config -> {
            selectedSourceUri.set(config.getSourceUri());
            return parsed.toBuilder()
                    .workbookPath(config.getSourceUri())
                    .build();
        };
        when(parserProvider.getParser(DataSourceType.EXCEL)).thenReturn(parser);
        when(standardizationService.standardize(any())).thenAnswer(invocation ->
                ((ParsedValuationData) invocation.getArgument(0)).toBuilder()
                        .fileNameOriginal("source.xlsx")
                        .build());

        service.execute(99L);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskGateway).markSuccess(eq(99L), payloadCaptor.capture());
        assertThat(selectedSourceUri.get()).isEqualTo(realStorageFile.toString());
        assertThat(payloadCaptor.getValue()).contains("\"workbookPath\":\"" + realStorageFile + "\"");
    }

    @Test
    void execute_should_fail_when_parser_returns_missing_row_numbers() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ParseExecutionAppServiceImpl service = new ParseExecutionAppServiceImpl(
                taskGateway,
                parserProvider,
                dwdExternalValuationGateway,
                standardizedExternalValuationGateway,
                dwdJjhzgzbGateway,
                trIndexGateway,
                valsetFileInfoGateway,
                standardizationService,
                objectMapper,
                tracer,
                null
        );

        WorkflowTask workflowTask = WorkflowTask.builder()
                .taskId(99L)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(TaskStatus.PENDING)
                .taskStage(TaskStage.PARSE)
                .businessKey("PARSE:EXCEL:12")
                .fileId(12L)
                .inputPayload(objectMapper.writeValueAsString(java.util.Map.of(
                        "workbookPath", "/tmp/source.xlsx",
                        "dataSourceType", DataSourceType.EXCEL.name(),
                        "fileId", 12L,
                        "createdBy", "tester"
                )))
                .taskStartTime(LocalDateTime.now())
                .build();

        ValsetFileInfo fileInfo = ValsetFileInfo.builder()
                .fileId(12L)
                .fileNameOriginal("source.xlsx")
                .localTempPath("/tmp/valset-parse-missing-" + System.nanoTime() + ".xlsx")
                .realStoragePath("/tmp/valset-parse-real-" + System.nanoTime() + ".xlsx")
                .build();

        ParsedValuationData parsed = ParsedValuationData.builder()
                .sheetName("Sheet1")
                .subjects(List.of(SubjectRecord.builder().subjectCode("1001").subjectName("现金").build()))
                .metrics(List.of())
                .build();

        when(tracer.nextSpan()).thenReturn(rootSpan, childSpan, childSpan);
        when(rootSpan.name(any())).thenReturn(rootSpan);
        when(rootSpan.tag(any(), any())).thenReturn(rootSpan);
        when(rootSpan.start()).thenReturn(rootSpan);
        when(childSpan.name(any())).thenReturn(childSpan);
        when(childSpan.start()).thenReturn(childSpan);
        when(tracer.withSpan(any())).thenReturn(spanInScope);
        doNothing().when(rootSpan).end();
        doNothing().when(childSpan).end();

        when(taskGateway.findById(99L)).thenReturn(workflowTask);
        when(valsetFileInfoGateway.findById(12L)).thenReturn(fileInfo);
        when(parserProvider.getParser(DataSourceType.EXCEL)).thenReturn(config -> parsed.toBuilder()
                .workbookPath(config.getSourceUri())
                .build());

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to execute parse task")
                .hasRootCauseMessage("解析失败，未识别表头行号或数据起始行号，fileId=12, headerRowNumber=null, dataStartRowNumber=null");

        verify(dwdExternalValuationGateway, never()).saveDwdExternalValuation(anyLong(), anyLong(), any());
        verify(standardizationService, never()).standardize(any());
    }
}
