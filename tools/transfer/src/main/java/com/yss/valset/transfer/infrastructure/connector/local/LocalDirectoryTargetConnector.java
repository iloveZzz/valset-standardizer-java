package com.yss.valset.transfer.infrastructure.connector.local;

import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.LocalDirectoryTargetConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地目录投递连接器。
 */
@Component
public class LocalDirectoryTargetConnector implements TargetConnector {

    @Override
    public String type() {
        return TargetType.LOCAL_DIR.name();
    }

    @Override
    public boolean supports(TransferTarget target) {
        return target != null && target.targetType() == TargetType.LOCAL_DIR;
    }

    @Override
    public TransferResult send(TransferContext context) {
        TransferObject transferObject = requireTransferObject(context);
        LocalDirectoryTargetConfig config = LocalDirectoryTargetConfig.from(context);
        Path sourcePath = resolveSourcePath(transferObject);
        Path targetPath = resolveTargetPath(context, config, transferObject);

        try {
            if (config.createParentDirectories()) {
                Path parent = targetPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } else {
                Path parent = targetPath.getParent();
                if (parent != null && Files.notExists(parent)) {
                    throw new IllegalStateException("本地目录目标父目录不存在，path=" + parent);
                }
            }
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new TransferResult(true, List.of("本地目录投递成功，path=" + targetPath.toAbsolutePath()));
        } catch (IOException e) {
            throw new IllegalStateException("本地目录投递失败，path=" + targetPath.toAbsolutePath(), e);
        }
    }

    private TransferObject requireTransferObject(TransferContext context) {
        if (context == null || context.transferObject() == null) {
            throw new IllegalArgumentException("本地目录投递失败，缺少文件对象");
        }
        return context.transferObject();
    }

    private Path resolveSourcePath(TransferObject transferObject) {
        String localTempPath = firstNonBlank(transferObject.localTempPath(), transferObject.sourceRef());
        if (localTempPath == null) {
            throw new IllegalStateException("本地目录投递失败，缺少可投递的本地文件路径");
        }
        Path sourcePath = Paths.get(localTempPath);
        if (Files.notExists(sourcePath)) {
            throw new IllegalStateException("本地目录投递失败，源文件不存在，path=" + sourcePath.toAbsolutePath());
        }
        return sourcePath;
    }

    private Path resolveTargetPath(TransferContext context, LocalDirectoryTargetConfig config, TransferObject transferObject) {
        String directoryText = resolveTemplate(config.directory(), context, transferObject);
        Path directory = Paths.get(directoryText);
        String targetPathTemplate = firstNonBlank(
                attributeText(context, TransferConfigKeys.TARGET_PATH),
                context.transferTarget() == null ? null : context.transferTarget().targetPathTemplate()
        );
        if (targetPathTemplate != null && !targetPathTemplate.isBlank()) {
            String resolvedTargetPath = resolveTemplate(targetPathTemplate, context, transferObject);
            Path targetPath = Paths.get(resolvedTargetPath);
            if (!targetPath.isAbsolute()) {
                directory = directory.resolve(targetPath);
            } else {
                directory = targetPath;
            }
        }
        String fileNameTemplate = firstNonBlank(
                attributeText(context, TransferConfigKeys.RENAME_PATTERN),
                transferObject.originalName()
        );
        String resolvedFileName = resolveTemplate(fileNameTemplate, context, transferObject);
        if (resolvedFileName == null || resolvedFileName.isBlank()) {
            resolvedFileName = transferObject.originalName();
        }
        return directory.resolve(resolvedFileName).normalize();
    }

    private String resolveTemplate(String template, TransferContext context, TransferObject transferObject) {
        if (template == null || template.isBlank()) {
            return template;
        }
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("fileName", firstNonBlank(transferObject.originalName(), ""));
        variables.put("originalName", firstNonBlank(transferObject.originalName(), ""));
        variables.put("sourceCode", firstNonBlank(transferObject.sourceCode(), ""));
        variables.put("sourceId", firstNonBlank(transferObject.sourceId(), ""));
        variables.put("sourceType", firstNonBlank(transferObject.sourceType(), ""));
        variables.put("targetCode", context.transferTarget() == null ? "" : firstNonBlank(context.transferTarget().targetCode(), ""));
        variables.put("routeId", context.transferRoute() == null ? "" : firstNonBlank(context.transferRoute().routeId(), ""));
        variables.put("transferId", firstNonBlank(transferObject.transferId(), ""));
        variables.put("extension", firstNonBlank(transferObject.extension(), ""));
        variables.put("mimeType", firstNonBlank(transferObject.mimeType(), ""));
        variables.put("yyyyMMdd", LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE));
        variables.put("yyyy-MM-dd", LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE));

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String attributeText(TransferContext context, String key) {
        if (context == null || context.attributes() == null) {
            return routeText(context, key);
        }
        Object raw = context.attributes().get(key);
        if (raw != null && !String.valueOf(raw).isBlank()) {
            return String.valueOf(raw);
        }
        return routeText(context, key);
    }

    private String routeText(TransferContext context, String key) {
        if (context == null || context.transferRoute() == null) {
            return targetText(context, key);
        }
        if (TransferConfigKeys.TARGET_PATH.equals(key) && context.transferRoute().targetPath() != null && !context.transferRoute().targetPath().isBlank()) {
            return context.transferRoute().targetPath();
        }
        if (TransferConfigKeys.RENAME_PATTERN.equals(key) && context.transferRoute().renamePattern() != null && !context.transferRoute().renamePattern().isBlank()) {
            return context.transferRoute().renamePattern();
        }
        return targetText(context, key);
    }

    private String targetText(TransferContext context, String key) {
        if (context == null || context.transferTarget() == null) {
            return null;
        }
        if (TransferConfigKeys.TARGET_PATH.equals(key)
                && context.transferTarget().targetPathTemplate() != null
                && !context.transferTarget().targetPathTemplate().isBlank()) {
            return context.transferTarget().targetPathTemplate();
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
