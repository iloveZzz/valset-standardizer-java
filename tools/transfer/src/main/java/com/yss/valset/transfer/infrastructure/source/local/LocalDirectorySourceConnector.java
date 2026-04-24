package com.yss.valset.transfer.infrastructure.source.local;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.LocalDirectorySourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import lombok.RequiredArgsConstructor;

/**
 * 本地目录来源连接器。
 */
@Component
@RequiredArgsConstructor
public class LocalDirectorySourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(LocalDirectorySourceConnector.class);
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;

    @Override
    public String type() {
        return SourceType.LOCAL_DIR.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.LOCAL_DIR;
    }

    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        LocalDirectorySourceConfig config = LocalDirectorySourceConfig.from(source);
        Path directory = resolveDirectory(config);
        String cursor = readCursor(source);

        try (Stream<Path> stream = config.recursive() ? Files.walk(directory) : Files.list(directory)) {
            List<Path> visibleFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> includeHidden(config, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            SourceFetchLogSupport.logStart(log, "本地目录", source, "directory", directory, "文件总数", visibleFiles.size());
            Stream<Path> fileStream = visibleFiles.stream();
            if (config.limit() > 0) {
                fileStream = fileStream.limit(config.limit());
            }
            List<RecognitionContext> contexts = new ArrayList<>();
            boolean seenCursor = cursor == null || cursor.isBlank();
            for (Path path : fileStream.toList()) {
                if (shouldStop(source)) {
                    break;
                }
                RecognitionContext context = toContext(source, path, cursor, seenCursor);
                if (context != null) {
                    contexts.add(context);
                    seenCursor = true;
                }
            }
            return contexts;
        } catch (IOException e) {
            throw new IllegalStateException("扫描本地目录失败，directory=" + directory, e);
        }
    }

    private RecognitionContext toContext(TransferSource source, Path path, String cursor, boolean seenCursor) throws IOException {
        long size = Files.size(path);
        long lastModified = Files.getLastModifiedTime(path).toMillis();
        String checkpointKey = buildCheckpointKey(path, lastModified, size);
        if (cursor != null && !cursor.isBlank() && !seenCursor && !cursor.equals(checkpointKey)) {
            return null;
        }
        if (source.sourceId() != null && transferSourceCheckpointGateway.existsProcessedItem(source.sourceId(), checkpointKey)) {
            return null;
        }
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("size", size);
        attrs.put("lastModified", lastModified);
        attrs.put("directory", path.getParent() == null ? null : path.getParent().toString());
        attrs.put("absolutePath", path.toAbsolutePath().toString());
        attrs.put(TransferConfigKeys.CHECKPOINT_KEY, checkpointKey);
        attrs.put(TransferConfigKeys.CHECKPOINT_REF, path.toAbsolutePath().toString());
        attrs.put(TransferConfigKeys.CHECKPOINT_NAME, path.getFileName().toString());
        attrs.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, checkpointKey);
        return new RecognitionContext(
                SourceType.LOCAL_DIR,
                source.sourceCode(),
                path.getFileName().toString(),
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

    private String readCursor(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return null;
        }
        return transferSourceCheckpointGateway.findCheckpoint(source.sourceId(), TransferConfigKeys.CHECKPOINT_SCAN_CURSOR)
                .map(checkpoint -> checkpoint.checkpointValue())
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }

    private String buildCheckpointKey(Path path, long lastModified, long size) {
        return path.toAbsolutePath() + "|" + lastModified + "|" + size;
    }

    private Path resolveDirectory(LocalDirectorySourceConfig config) {
        String directoryText = config.directory();
        if (directoryText == null || directoryText.isBlank()) {
            throw new IllegalArgumentException("本地目录来源缺少 " + TransferConfigKeys.DIRECTORY + " 配置");
        }
        Path directory = Paths.get(directoryText);
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("本地目录不存在或不是目录，directory=" + directory);
        }
        return directory;
    }

    private boolean includeHidden(LocalDirectorySourceConfig config, Path path) {
        return config.includeHidden() || !path.getFileName().toString().startsWith(".");
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
