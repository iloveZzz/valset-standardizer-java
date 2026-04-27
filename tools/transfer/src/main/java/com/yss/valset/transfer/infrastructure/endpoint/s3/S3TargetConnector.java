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
        return new TransferResult(true, null, messages);
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

    private String buildObjectKey(S3TargetConfig config, TransferContext context) {
        TransferObject transferObject = context.transferObject();
        String basePath = firstNonBlank(
                stringValue(context.transferRoute().routeMeta(), TransferConfigKeys.TARGET_PATH, null),
                stringValue(context.transferTarget() == null ? null : context.transferTarget().targetMeta(), TransferConfigKeys.TARGET_PATH, null),
                stringValue(context.transferTarget() == null ? null : context.transferTarget().targetMeta(), TransferConfigKeys.KEY_PREFIX, null),
                context.transferTarget() == null ? null : context.transferTarget().targetPathTemplate(),
                config.keyPrefix()
        );
        String fileName = firstNonBlank(transferObject.originalName(), "transfer-file");
        if (basePath.isBlank()) {
            return fileName;
        }
        if (basePath.endsWith("/")) {
            return basePath + fileName;
        }
        return basePath;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config == null ? null : config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }
}
