package com.yss.valset.transfer.infrastructure.endpoint.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.S3TargetConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S3 投递连接器。
 */
@Component
public class S3TargetConnector implements TargetConnector {

    @Override
    public String type() {
        return TargetType.S3.name();
    }

    @Override
    public boolean supports(TransferTarget target) {
        return target != null && target.targetType() == TargetType.S3;
    }

    @Override
    public TransferResult send(TransferContext context) {
        S3TargetConfig config = S3TargetConfig.from(context);
        TransferObject transferObject = context.transferObject();
        File file = new File(transferObject.localTempPath());
        if (!file.isFile()) {
            return new TransferResult(false, null, Collections.singletonList("未找到待投递文件: " + transferObject.localTempPath()));
        }
        String objectKey = buildObjectKey(config, context);
        AmazonS3 s3Client = buildClient(config);
        ObjectMetadata metadata = new ObjectMetadata();
        if (transferObject.sizeBytes() != null && transferObject.sizeBytes() >= 0) {
            metadata.setContentLength(transferObject.sizeBytes());
        }
        s3Client.putObject(new PutObjectRequest(config.bucket(), objectKey, file).withMetadata(metadata));
        List<String> messages = new ArrayList<>();
        messages.add("S3 投递成功");
        messages.add("bucket=" + config.bucket());
        messages.add("objectKey=" + objectKey);
        return new TransferResult(true, null, objectKey, messages);
    }

    private AmazonS3 buildClient(S3TargetConfig config) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        if (config.accessKey() != null && !config.accessKey().isBlank() && config.secretKey() != null && !config.secretKey().isBlank()) {
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.accessKey(), config.secretKey())));
        }
        if (config.endpointUrl() != null && !config.endpointUrl().isBlank()) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(config.endpointUrl(), config.region()));
        } else if (config.region() != null && !config.region().isBlank()) {
            builder.withRegion(config.region());
        }
        if (config.usePathStyle()) {
            builder.setPathStyleAccessEnabled(true);
        }
        return builder.build();
    }

    String buildObjectKey(S3TargetConfig config, TransferContext context) {
        TransferObject transferObject = context.transferObject();
        String basePath = firstNonBlank(
                stringValue(context.transferRoute().routeMeta(), TransferConfigKeys.TARGET_PATH, null),
                stringValue(context.transferTarget() == null ? null : context.transferTarget().targetMeta(), TransferConfigKeys.TARGET_PATH, null),
                stringValue(context.transferTarget() == null ? null : context.transferTarget().targetMeta(), TransferConfigKeys.KEY_PREFIX, null),
                context.transferTarget() == null ? null : context.transferTarget().targetPathTemplate(),
                config.keyPrefix()
        );
        String fileName = firstNonBlank(transferObject.originalName(), "transfer-file");
        String resolvedBasePath = resolveTemplate(basePath, context, transferObject);
        if (resolvedBasePath == null || resolvedBasePath.isBlank()) {
            return fileName;
        }
        return joinPath(resolvedBasePath, fileName);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String joinPath(String basePath, String fileName) {
        if (basePath == null || basePath.isBlank()) {
            return fileName;
        }
        String normalizedBasePath = basePath.endsWith("/") && basePath.length() > 1
                ? basePath.substring(0, basePath.length() - 1)
                : basePath;
        if (normalizedBasePath.endsWith("/")) {
            return normalizedBasePath + fileName;
        }
        return normalizedBasePath + "/" + fileName;
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

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config == null ? null : config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }
}
