package com.yss.valset.transfer.infrastructure.source.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.S3SourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;

/**
 * S3 来源连接器，支持从对象存储目录拉取文件并转入临时文件。
 */
@Component
@RequiredArgsConstructor
public class S3SourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(S3SourceConnector.class);
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;

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
        S3SourceConfig config = S3SourceConfig.from(source);
        AmazonS3 s3Client = buildClient(config);
        List<RecognitionContext> contexts = new ArrayList<>();
        String cursor = readCursor(source);
        try {
            List<S3ObjectSummary> summaries = listObjects(s3Client, config);
            summaries.sort(Comparator.comparing(S3ObjectSummary::getKey));
            SourceFetchLogSupport.logStart(log, "S3", source, "bucket", config.bucket(), "对象总数", summaries.size());
            boolean seenCursor = cursor == null || cursor.isBlank();
            for (S3ObjectSummary summary : summaries) {
                if (shouldStop(source)) {
                    break;
                }
                if (config.limit() > 0 && contexts.size() >= config.limit()) {
                    break;
                }
                if (summary.getKey() == null || summary.getKey().isBlank() || summary.getKey().endsWith("/")) {
                    continue;
                }
                String objectName = lastPathSegment(summary.getKey());
                String checkpointKey = buildCheckpointKey(config.bucket(), summary.getKey(), summary.getETag(), summary.getSize(), summary.getLastModified() == null ? null : summary.getLastModified().toInstant().toString());
                if (!seenCursor) {
                    if (cursor.equals(checkpointKey)) {
                        seenCursor = true;
                    }
                    continue;
                }
                if (source.sourceId() != null && transferSourceCheckpointGateway.existsProcessedItem(source.sourceId(), checkpointKey)) {
                    continue;
                }
                Path tempFile = Files.createTempFile("transfer-s3-", buildTempSuffix(objectName));
                try (S3Object s3Object = s3Client.getObject(config.bucket(), summary.getKey());
                     InputStream inputStream = s3Object.getObjectContent()) {
                    Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Map<String, Object> attrs = new LinkedHashMap<>();
                attrs.put(TransferConfigKeys.BUCKET, config.bucket());
                attrs.put(TransferConfigKeys.OBJECT_KEY, summary.getKey());
                attrs.put(TransferConfigKeys.REMOTE_PATH, config.bucket() + ":" + summary.getKey());
                attrs.put(TransferConfigKeys.E_TAG, summary.getETag());
                attrs.put(TransferConfigKeys.CHECKPOINT_KEY, checkpointKey);
                attrs.put(TransferConfigKeys.CHECKPOINT_REF, config.bucket() + ":" + summary.getKey());
                attrs.put(TransferConfigKeys.CHECKPOINT_NAME, objectName);
                attrs.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, checkpointKey);
                attrs.put("size", summary.getSize());
                attrs.put("lastModified", summary.getLastModified() == null ? null : summary.getLastModified().toInstant().toString());
                attrs.put("tempPath", tempFile.toAbsolutePath().toString());
                contexts.add(new RecognitionContext(
                        SourceType.S3,
                        config.sourceCode(),
                        objectName,
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

    private String readCursor(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return null;
        }
        return transferSourceCheckpointGateway.findCheckpoint(source.sourceId(), TransferConfigKeys.CHECKPOINT_SCAN_CURSOR)
                .map(checkpoint -> checkpoint.checkpointValue())
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }

    private List<S3ObjectSummary> listObjects(AmazonS3 s3Client, S3SourceConfig config) {
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

    private AmazonS3 buildClient(S3SourceConfig config) {
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

    private String buildTempSuffix(String fileName) {
        String sanitized = sanitizeFileName(fileName);
        String extension = "";
        int lastDot = sanitized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < sanitized.length() - 1) {
            extension = sanitized.substring(lastDot);
        }
        return "-" + shortHash(sanitized) + extension;
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(hashed.length, 6); i++) {
                builder.append(String.format("%02x", hashed[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value == null ? 0 : value.hashCode());
        }
    }

    private String buildCheckpointKey(String bucket, String objectKey, String etag, long size, String lastModified) {
        return String.join("|",
                Objects.toString(bucket, ""),
                Objects.toString(objectKey, ""),
                Objects.toString(etag, ""),
                String.valueOf(size),
                Objects.toString(lastModified, ""));
    }

    private boolean shouldStop(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return false;
        }
        return transferSourceGateway.findById(source.sourceId())
                .map(current -> "STOPPING".equalsIgnoreCase(current.ingestStatus())
                        || "STOPPED".equalsIgnoreCase(current.ingestStatus()))
                .orElse(false);
    }

}
