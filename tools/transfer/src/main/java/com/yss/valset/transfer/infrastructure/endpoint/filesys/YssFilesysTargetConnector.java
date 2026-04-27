package com.yss.valset.transfer.infrastructure.endpoint.filesys;

import com.yss.filesys.feignsdk.dto.YssFilesysUploadFlowResult;
import com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties;
import com.yss.filesys.feignsdk.service.YssFilesysUploadFlowService;
import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.FilesysTargetConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * yss-filesys 存储目标连接器。
 */
@Component
public class YssFilesysTargetConnector implements TargetConnector {

    private final YssFilesysUploadFlowService uploadFlowService;
    private final YssFilesysFeignSdkProperties properties;

    public YssFilesysTargetConnector(YssFilesysUploadFlowService uploadFlowService,
                                     YssFilesysFeignSdkProperties properties) {
        this.uploadFlowService = uploadFlowService;
        this.properties = properties;
    }

    @Override
    public String type() {
        return TargetType.FILESYS.name();
    }

    @Override
    public boolean supports(TransferTarget target) {
        return target != null && target.targetType() == TargetType.FILESYS;
    }

    @Override
    public TransferResult send(TransferContext context) {
        FilesysTargetConfig config = FilesysTargetConfig.from(context, properties.getDefaultChunkSize());
        TransferObject transferObject = context.transferObject();
        if (transferObject.localTempPath() == null || transferObject.localTempPath().isBlank()) {
            return new TransferResult(false, null, List.of("未找到待上传文件路径"));
        }
        Path localFile = Path.of(transferObject.localTempPath());
        if (!Files.isRegularFile(localFile)) {
            return new TransferResult(false, null, List.of("未找到待上传文件: " + transferObject.localTempPath()));
        }

        try {
            byte[] content = Files.readAllBytes(localFile);
            String fileName = firstNonBlank(transferObject.originalName(), localFile.getFileName().toString());
            String mimeType = firstNonBlank(transferObject.mimeType(), Files.probeContentType(localFile), "application/octet-stream");
            String directoryPath = resolveDirectoryPath(context);
            YssFilesysUploadFlowResult result = uploadFlowService.upload(
                    content,
                    fileName,
                    mimeType,
                    config.parentId(),
                    directoryPath,
                    config.storageSettingId(),
                    config.chunkSize()
            );
            List<String> messages = new ArrayList<>();
            messages.add("yss-filesys 上传成功");
            messages.add("taskId=" + result.getTaskId());
            messages.add("directoryPath=" + directoryPath);
            String fileId = null;
            if (result.getFileRecord() != null) {
                fileId = result.getFileRecord().getFileId();
                messages.add("fileId=" + fileId);
                messages.add("objectKey=" + result.getFileRecord().getObjectKey());
                messages.add("storageSettingId=" + result.getFileRecord().getStorageSettingId());
            }
            return new TransferResult(true, fileId, messages);
        } catch (IOException e) {
            throw new IllegalStateException("上传文件到 yss-filesys 失败", e);
        }
    }

    private String resolveDirectoryPath(TransferContext context) {
        String configuredPath = firstNonBlank(
                context.transferRoute() == null ? null : context.transferRoute().targetPath(),
                context.transferTarget() == null ? null : context.transferTarget().targetPathTemplate()
        );
        if (configuredPath != null && !configuredPath.isBlank()) {
            return resolveTemplate(configuredPath, context);
        }
        return LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String resolveTemplate(String template, TransferContext context) {
        if (template == null || template.isBlank()) {
            return template;
        }
        TransferObject transferObject = context.transferObject();
        String result = template;
        result = result.replace("${fileName}", firstNonBlank(transferObject.originalName(), ""));
        result = result.replace("${originalName}", firstNonBlank(transferObject.originalName(), ""));
        result = result.replace("${sourceCode}", firstNonBlank(transferObject.sourceCode(), ""));
        result = result.replace("${sourceId}", firstNonBlank(transferObject.sourceId(), ""));
        result = result.replace("${transferId}", firstNonBlank(transferObject.transferId(), ""));
        result = result.replace("${yyyyMMdd}", LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE));
        result = result.replace("${yyyy-MM-dd}", LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE));
        return result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

}
