package com.yss.valset.transfer.infrastructure.source.http;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.HttpSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * HTTP 来源连接器，负责从 HTTP 上传目录扫描待处理文件。
 */
@Component
@RequiredArgsConstructor
public class HttpSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(HttpSourceConnector.class);
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;

    @Override
    public String type() {
        return SourceType.HTTP.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.HTTP;
    }

    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        HttpSourceConfig config = HttpSourceConfig.from(source);
        Path directory = resolveDirectory(source);
        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> visibleFiles = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            SourceFetchLogSupport.logStart(log, "HTTP", source, "directory", directory, "文件总数", visibleFiles.size());
            Stream<Path> fileStream = visibleFiles.stream();
            if (config.limit() > 0) {
                fileStream = fileStream.limit(config.limit());
            }
            List<RecognitionContext> contexts = new ArrayList<>();
            for (Path path : fileStream.toList()) {
                if (shouldStop(source)) {
                    break;
                }
                RecognitionContext context = toContext(source, path);
                if (context != null) {
                    contexts.add(context);
                }
            }
            return contexts;
        } catch (IOException exception) {
            throw new IllegalStateException("扫描 HTTP 上传目录失败，directory=" + directory, exception);
        }
    }

    private RecognitionContext toContext(TransferSource source, Path path) throws IOException {
        long size = Files.size(path);
        long lastModified = Files.getLastModifiedTime(path).toMillis();
        String checkpointKey = buildCheckpointKey(path, lastModified, size);
        if (source.sourceId() != null && transferSourceCheckpointGateway.existsProcessedItem(source.sourceId(), checkpointKey)) {
            return null;
        }
        String fileName = path.getFileName().toString();
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("materializedByConnector", Boolean.TRUE);
        attrs.put("uploadDirectory", path.getParent() == null ? null : path.getParent().toString());
        attrs.put("absolutePath", path.toAbsolutePath().toString());
        attrs.put("lastModified", lastModified);
        attrs.put(TransferConfigKeys.CHECKPOINT_KEY, checkpointKey);
        attrs.put(TransferConfigKeys.CHECKPOINT_REF, path.toAbsolutePath().toString());
        attrs.put(TransferConfigKeys.CHECKPOINT_NAME, fileName);
        attrs.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, checkpointKey);
        return new RecognitionContext(
                SourceType.HTTP,
                source.sourceCode(),
                fileName,
                Files.probeContentType(path),
                size,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                path.toAbsolutePath().toString(),
                attrs
        );
    }

    private String buildCheckpointKey(Path path, long lastModified, long size) {
        return path.toAbsolutePath() + "|" + lastModified + "|" + size;
    }

    private Path resolveDirectory(TransferSource source) {
        if (source == null || source.sourceId() == null || source.sourceId().isBlank()) {
            throw new IllegalArgumentException("HTTP 来源缺少 sourceId");
        }
        Path directory = Paths.get(System.getProperty("user.home"), ".tmp", "valset-standardizer", "uploads", "http", source.sourceId());
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                throw new IllegalStateException("创建 HTTP 来源上传目录失败，directory=" + directory, exception);
            }
        }
        return directory;
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
