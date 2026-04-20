package com.yss.valset.transfer.infrastructure.source.local;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
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
 * 本地目录来源连接器。
 */
@Component
public class LocalDirectorySourceConnector implements SourceConnector {

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
        Path directory = resolveDirectory(source);
        boolean recursive = booleanParam(source, "recursive", false);
        int limit = intParam(source, "limit", 0);

        try (Stream<Path> stream = recursive ? Files.walk(directory) : Files.list(directory)) {
            Stream<Path> fileStream = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> includeHidden(source, path))
                    .sorted(Comparator.comparing(Path::toString));
            if (limit > 0) {
                fileStream = fileStream.limit(limit);
            }
            List<RecognitionContext> contexts = new ArrayList<>();
            for (Path path : fileStream.toList()) {
                contexts.add(toContext(source, path));
            }
            return contexts;
        } catch (IOException e) {
            throw new IllegalStateException("扫描本地目录失败，directory=" + directory, e);
        }
    }

    private RecognitionContext toContext(TransferSource source, Path path) throws IOException {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("size", Files.size(path));
        attrs.put("lastModified", Files.getLastModifiedTime(path).toMillis());
        attrs.put("directory", path.getParent() == null ? null : path.getParent().toString());
        attrs.put("absolutePath", path.toAbsolutePath().toString());
        return new RecognitionContext(
                SourceType.LOCAL_DIR,
                source.sourceCode(),
                path.getFileName().toString(),
                Files.probeContentType(path),
                Files.size(path),
                null,
                null,
                path.toAbsolutePath().toString(),
                attrs
        );
    }

    private Path resolveDirectory(TransferSource source) {
        Object configuredDirectory = source.connectionConfig() == null ? null : source.connectionConfig().get("directory");
        String directoryText = configuredDirectory == null ? null : String.valueOf(configuredDirectory);
        if (directoryText == null || directoryText.isBlank()) {
            directoryText = source.sourceCode();
        }
        if (directoryText == null || directoryText.isBlank()) {
            throw new IllegalArgumentException("本地目录来源缺少 directory 配置");
        }
        Path directory = Paths.get(directoryText);
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("本地目录不存在或不是目录，directory=" + directory);
        }
        return directory;
    }

    private boolean includeHidden(TransferSource source, Path path) {
        boolean includeHidden = booleanParam(source, "includeHidden", false);
        return includeHidden || !path.getFileName().toString().startsWith(".");
    }

    private boolean booleanParam(TransferSource source, String key, boolean defaultValue) {
        Object raw = readConfig(source, key);
        if (raw == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    private int intParam(TransferSource source, String key, int defaultValue) {
        Object raw = readConfig(source, key);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数不是有效整数，key=" + key + ", value=" + raw, e);
        }
    }

    private Object readConfig(TransferSource source, String key) {
        if (source.connectionConfig() != null && source.connectionConfig().containsKey(key)) {
            return source.connectionConfig().get(key);
        }
        if (source.sourceMeta() != null && source.sourceMeta().containsKey(key)) {
            return source.sourceMeta().get(key);
        }
        return null;
    }
}
