package com.yss.valset.transfer.infrastructure.source.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * S3 来源连接器，支持从对象存储目录拉取文件并转入临时文件。
 */
@Component
public class S3SourceConnector implements SourceConnector {

    @Override
    public String type() {
        return SourceType.S3.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.S3;
    }

    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        S3Config config = S3Config.from(source);
        AmazonS3 s3Client = buildClient(config);
        List<RecognitionContext> contexts = new ArrayList<>();
        try {
            List<S3ObjectSummary> summaries = listObjects(s3Client, config);
            for (S3ObjectSummary summary : summaries) {
                if (config.limit() > 0 && contexts.size() >= config.limit()) {
                    break;
                }
                if (summary.getKey() == null || summary.getKey().isBlank() || summary.getKey().endsWith("/")) {
                    continue;
                }
                Path tempFile = Files.createTempFile("transfer-s3-", "-" + sanitizeFileName(lastPathSegment(summary.getKey())));
                try (S3Object s3Object = s3Client.getObject(config.bucket(), summary.getKey());
                     InputStream inputStream = s3Object.getObjectContent()) {
                    Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Map<String, Object> attrs = new LinkedHashMap<>();
                attrs.put("bucket", config.bucket());
                attrs.put("objectKey", summary.getKey());
                attrs.put("remotePath", config.bucket() + ":" + summary.getKey());
                attrs.put("etag", summary.getETag());
                attrs.put("size", summary.getSize());
                attrs.put("lastModified", summary.getLastModified() == null ? null : summary.getLastModified().toInstant().toString());
                attrs.put("tempPath", tempFile.toAbsolutePath().toString());
                contexts.add(new RecognitionContext(
                        SourceType.S3,
                        config.sourceCode(),
                        lastPathSegment(summary.getKey()),
                        null,
                        summary.getSize(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        tempFile.toAbsolutePath().toString(),
                        attrs
                ));
            }
            return contexts;
        } catch (Exception e) {
            throw new IllegalStateException("收取 S3 文件失败，bucket=" + config.bucket() + ", prefix=" + config.prefix(), e);
        }
    }

    private List<S3ObjectSummary> listObjects(AmazonS3 s3Client, S3Config config) {
        List<S3ObjectSummary> summaries = new ArrayList<>();
        String prefix = config.prefix() == null ? "" : config.prefix();
        String marker = null;
        do {
            var request = new com.amazonaws.services.s3.model.ListObjectsV2Request()
                    .withBucketName(config.bucket())
                    .withPrefix(prefix)
                    .withContinuationToken(marker);
            var result = s3Client.listObjectsV2(request);
            summaries.addAll(result.getObjectSummaries());
            marker = result.isTruncated() ? result.getNextContinuationToken() : null;
        } while (marker != null);
        return summaries;
    }

    private AmazonS3 buildClient(S3Config config) {
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

    private String lastPathSegment(String key) {
        if (key == null || key.isBlank()) {
            return "transfer-file";
        }
        int index = key.lastIndexOf('/');
        return index < 0 ? key : key.substring(index + 1);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record S3Config(
            String bucket,
            String region,
            String endpointUrl,
            String accessKey,
            String secretKey,
            boolean usePathStyle,
            String prefix,
            int limit,
            String sourceCode
    ) {
        static S3Config from(TransferSource source) {
            Map<String, Object> config = source.connectionConfig() == null ? Collections.emptyMap() : source.connectionConfig();
            String bucket = requiredString(config, "bucket");
            String region = stringValue(config, "region", "cn-north-1");
            String endpointUrl = stringValue(config, "endpointUrl", null);
            String accessKey = stringValue(config, "accessKey", null);
            String secretKey = stringValue(config, "secretKey", null);
            boolean usePathStyle = booleanValue(config, "usePathStyle", false);
            String prefix = firstNonBlank(stringValue(config, "prefix", null), stringValue(config, "keyPrefix", null));
            int limit = intValue(config, "limit", 0);
            String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank() ? bucket : source.sourceCode();
            return new S3Config(bucket, region, endpointUrl, accessKey, secretKey, usePathStyle, prefix, limit, sourceCode);
        }

        private static String requiredString(Map<String, Object> config, String key) {
            Object raw = config.get(key);
            if (raw == null || String.valueOf(raw).isBlank()) {
                throw new IllegalArgumentException("S3 来源缺少必要配置: " + key);
            }
            return String.valueOf(raw);
        }

        private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
            Object raw = config == null ? null : config.get(key);
            return raw == null ? defaultValue : String.valueOf(raw);
        }

        private static int intValue(Map<String, Object> config, String key, int defaultValue) {
            Object raw = config.get(key);
            if (raw == null || String.valueOf(raw).isBlank()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(String.valueOf(raw));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("S3 来源参数不是有效整数，key=" + key + ", value=" + raw, e);
            }
        }

        private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
            Object raw = config.get(key);
            if (raw == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(String.valueOf(raw));
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }
}
