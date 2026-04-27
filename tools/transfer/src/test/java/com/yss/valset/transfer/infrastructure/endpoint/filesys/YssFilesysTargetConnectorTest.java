package com.yss.valset.transfer.infrastructure.endpoint.filesys;

import com.yss.filesys.feignsdk.dto.YssFilesysUploadFlowResult;
import com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties;
import com.yss.filesys.feignsdk.service.YssFilesysUploadFlowService;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YssFilesysTargetConnectorTest {

    @Test
    void should_use_today_directory_when_directory_path_is_blank() throws Exception {
        Path sourceFile = Files.createTempFile("valset-filesys-source-", ".txt");
        Files.writeString(sourceFile, "filesys payload", StandardCharsets.UTF_8);

        YssFilesysUploadFlowService uploadFlowService = mock(YssFilesysUploadFlowService.class);
        YssFilesysFeignSdkProperties properties = new YssFilesysFeignSdkProperties();
        properties.setDefaultChunkSize(8L * 1024 * 1024);
        when(uploadFlowService.upload(any(byte[].class), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(YssFilesysUploadFlowResult.builder()
                        .taskId("task-001")
                        .instantUpload(true)
                        .fileRecord(com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO.builder()
                                .fileId("file-001")
                                .build())
                        .build());

        YssFilesysTargetConnector connector = new YssFilesysTargetConnector(uploadFlowService, properties);
        TransferContext context = buildContext(sourceFile, "", "");

        TransferResult result = connector.send(context);

        String expectedDirectory = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(result.success()).isTrue();
        assertThat(result.fileId()).isEqualTo("file-001");
        verify(uploadFlowService).upload(
                any(byte[].class),
                eq(sourceFile.getFileName().toString()),
                eq("text/plain"),
                eq("parent-001"),
                eq(expectedDirectory),
                eq("storage-001"),
                eq(8L * 1024 * 1024)
        );
    }

    @Test
    void should_resolve_directory_template_from_target_path() throws Exception {
        Path sourceFile = Files.createTempFile("valset-filesys-source-", ".txt");
        Files.writeString(sourceFile, "filesys payload", StandardCharsets.UTF_8);

        YssFilesysUploadFlowService uploadFlowService = mock(YssFilesysUploadFlowService.class);
        YssFilesysFeignSdkProperties properties = new YssFilesysFeignSdkProperties();
        properties.setDefaultChunkSize(8L * 1024 * 1024);
        when(uploadFlowService.upload(any(byte[].class), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(YssFilesysUploadFlowResult.builder()
                        .taskId("task-002")
                        .instantUpload(true)
                        .fileRecord(com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO.builder()
                                .fileId("file-002")
                                .build())
                        .build());

        YssFilesysTargetConnector connector = new YssFilesysTargetConnector(uploadFlowService, properties);
        TransferContext context = buildContext(sourceFile, "archive/${yyyyMMdd}", "");

        TransferResult result = connector.send(context);

        String expectedDirectory = "archive/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(result.success()).isTrue();
        assertThat(result.fileId()).isEqualTo("file-002");
        verify(uploadFlowService).upload(
                any(byte[].class),
                eq(sourceFile.getFileName().toString()),
                eq("text/plain"),
                eq("parent-001"),
                eq(expectedDirectory),
                eq("storage-001"),
                eq(8L * 1024 * 1024)
        );
    }

    @Test
    void should_resolve_directory_template_with_context_variables() throws Exception {
        Path sourceFile = Files.createTempFile("valset-filesys-source-", ".txt");
        Files.writeString(sourceFile, "filesys payload", StandardCharsets.UTF_8);

        YssFilesysUploadFlowService uploadFlowService = mock(YssFilesysUploadFlowService.class);
        YssFilesysFeignSdkProperties properties = new YssFilesysFeignSdkProperties();
        properties.setDefaultChunkSize(8L * 1024 * 1024);
        when(uploadFlowService.upload(any(byte[].class), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(YssFilesysUploadFlowResult.builder()
                        .taskId("task-003")
                        .instantUpload(true)
                        .fileRecord(com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO.builder()
                                .fileId("file-003")
                                .build())
                        .build());

        YssFilesysTargetConnector connector = new YssFilesysTargetConnector(uploadFlowService, properties);
        TransferContext context = buildContext(sourceFile, "/archive/${sourceCode}/${transferId}/${fileName}", "");

        TransferResult result = connector.send(context);

        String expectedDirectory = "/archive/source-code/transfer-001/" + sourceFile.getFileName();
        assertThat(result.success()).isTrue();
        assertThat(result.fileId()).isEqualTo("file-003");
        verify(uploadFlowService).upload(
                any(byte[].class),
                eq(sourceFile.getFileName().toString()),
                eq("text/plain"),
                eq("parent-001"),
                eq(expectedDirectory),
                eq("storage-001"),
                eq(8L * 1024 * 1024)
        );
    }

    private TransferContext buildContext(Path sourceFile, String targetPathTemplate, String routePath) throws Exception {
        TransferTarget target = new TransferTarget(
                101L,
                "filesys-target",
                "文件服务目标",
                TargetType.FILESYS,
                true,
                targetPathTemplate,
                Map.of("parentId", "parent-001", "storageSettingId", "storage-001"),
                Map.of(),
                Instant.now(),
                Instant.now()
        );
        TransferRoute route = new TransferRoute(
                "route-001",
                "source-001",
                null,
                "source-code",
                "rule-001",
                TargetType.FILESYS,
                "filesys-target",
                null,
                routePath,
                null,
                true,
                TransferStatus.ROUTED,
                Map.of()
        );
        TransferObject transferObject = new TransferObject(
                "transfer-001",
                "source-001",
                "EMAIL",
                "source-code",
                sourceFile.getFileName().toString(),
                "txt",
                "text/plain",
                Files.size(sourceFile),
                "fingerprint-001",
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
                sourceFile.toString(),
                TransferStatus.RECEIVED,
                Instant.now(),
                Instant.now(),
                "route-001",
                null,
                new ProbeResult(true, "TXT", Map.of()),
                Map.of()
        );
        return new TransferContext(transferObject, route, target, Map.of());
    }
}
