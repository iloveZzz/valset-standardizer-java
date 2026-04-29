package com.yss.valset.batch.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.analysis.application.command.ParseQueueCompleteCommand;
import com.yss.valset.analysis.application.command.ParseQueueSubscribeCommand;
import com.yss.valset.analysis.application.dto.ParseQueueViewDTO;
import com.yss.valset.analysis.application.service.ParseQueueManagementAppService;
import com.yss.valset.analysis.domain.gateway.ParseQueueGateway;
import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.analysis.domain.model.ParseStatus;
import com.yss.valset.analysis.domain.model.ParseTriggerMode;
import com.yss.valset.batch.dispatcher.TaskDispatcher;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileStorageType;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ParseQueueObserverJobTest {

    @Test
    void shouldSubscribeExecuteAndCompletePendingQueue() {
        ParseQueueGateway parseQueueGateway = mock(ParseQueueGateway.class);
        ParseQueueManagementAppService parseQueueManagementAppService = mock(ParseQueueManagementAppService.class);
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        ValsetFileInfoGateway valsetFileInfoGateway = mock(ValsetFileInfoGateway.class);
        TaskGateway taskGateway = mock(TaskGateway.class);
        TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ParseQueue queue = new ParseQueue(
                "1001",
                "WORKFLOW:PARSE:EXCEL:88",
                "transfer-1",
                "sample.xlsx",
                "source-1",
                "HTTP",
                "source-code",
                "route-1",
                "delivery-1",
                "tag-1",
                "VALUATION_TABLE",
                "估值表",
                "IDENTIFIED",
                "DELIVERED",
                ParseStatus.PENDING,
                ParseTriggerMode.AUTO,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                "{\"forceRebuild\":false}",
                null,
                Instant.now(),
                Instant.now()
        );
        when(parseQueueGateway.listPendingQueues(10)).thenReturn(List.of(queue), List.of());
        when(parseQueueManagementAppService.subscribeQueue(any(), any(ParseQueueSubscribeCommand.class)))
                .thenAnswer(invocation -> {
                    ParseQueueSubscribeCommand command = invocation.getArgument(1);
                    return ParseQueueViewDTO.builder()
                            .queueId(invocation.getArgument(0))
                            .subscribedBy(command.getSubscribedBy())
                            .build();
                });
        when(transferObjectGateway.findById("transfer-1")).thenReturn(Optional.of(new TransferObject(
                "transfer-1",
                "source-1",
                "HTTP",
                "source-code",
                "sample.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                1024L,
                "fingerprint-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TransferStatus.IDENTIFIED,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                "route-1",
                null,
                null,
                Map.of(),
                "/tmp/sample.xlsx"
        )));
        when(valsetFileInfoGateway.findByFingerprint("fingerprint-1")).thenReturn(ValsetFileInfo.builder()
                .fileId(88L)
                .fileNameOriginal("sample.xlsx")
                .fileFormat("EXCEL")
                .storageUri("/tmp/sample.xlsx")
                .storageType(ValsetFileStorageType.LOCAL)
                .build());
        when(taskGateway.save(any(TaskInfo.class))).thenReturn(42L);
        when(taskGateway.findById(42L)).thenReturn(TaskInfo.builder()
                .taskId(42L)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(TaskStatus.SUCCESS)
                .resultPayload("{\"workbookPath\":\"/tmp/sample.xlsx\"}")
                .build());
        when(parseQueueManagementAppService.completeQueue(any(), any(ParseQueueCompleteCommand.class))).thenAnswer(invocation -> {
            ParseQueueViewDTO dto = ParseQueueViewDTO.builder().queueId(invocation.getArgument(0)).build();
            return dto;
        });

        ParseQueueObserverJob job = new ParseQueueObserverJob(
                parseQueueGateway,
                parseQueueManagementAppService,
                transferObjectGateway,
                valsetFileInfoGateway,
                taskGateway,
                taskDispatcher,
                objectMapper
        );

        job.observePendingQueues();

        verify(parseQueueGateway).listPendingQueues(10);
        verify(parseQueueManagementAppService).subscribeQueue(eq("1001"), any(ParseQueueSubscribeCommand.class));
        verify(taskGateway).save(any(TaskInfo.class));
        verify(taskDispatcher).dispatchTask(42L);
        verify(parseQueueManagementAppService).completeQueue(eq("1001"), any(ParseQueueCompleteCommand.class));
        verifyNoMoreInteractions(taskDispatcher);
    }
}
