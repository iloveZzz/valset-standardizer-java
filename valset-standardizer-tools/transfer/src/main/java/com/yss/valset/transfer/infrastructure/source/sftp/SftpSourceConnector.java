package com.yss.valset.transfer.infrastructure.source.sftp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.SftpSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;

/**
 * SFTP 来源连接器，支持从远程目录拉取文件并转入临时文件。
 */
@Component
@RequiredArgsConstructor
public class SftpSourceConnector implements SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(SftpSourceConnector.class);
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;

    @Override
    public String type() {
        return SourceType.SFTP.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.SFTP;
    }

    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        SftpSourceConfig config = SftpSourceConfig.from(source);
        List<RecognitionContext> contexts = new ArrayList<>();
        JSch jsch = new JSch();
        if (config.privateKeyPath() != null && !config.privateKeyPath().isBlank()) {
            try {
                if (config.passphrase() == null || config.passphrase().isBlank()) {
                    jsch.addIdentity(config.privateKeyPath());
                } else {
                    jsch.addIdentity(config.privateKeyPath(), config.passphrase());
                }
            } catch (JSchException e) {
                throw new IllegalStateException("加载 SFTP 私钥失败", e);
            }
        }

        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = jsch.getSession(config.username(), config.host(), config.port());
            if (config.password() != null && !config.password().isBlank()) {
                session.setPassword(config.password());
            }
            session.setConfig("StrictHostKeyChecking", config.strictHostKeyChecking() ? "yes" : "no");
            session.connect(config.connectTimeoutMillis());
            channelSftp = openSftpChannel(session, config.channelTimeoutMillis());
            String cursor = readCursor(source);
            AtomicBoolean seenCursor = new AtomicBoolean(cursor == null || cursor.isBlank());
            String remoteDir = normalizeRemoteDir(config.remoteDir());
            long totalFiles = countFiles(channelSftp, config, remoteDir);
            SourceFetchLogSupport.logStart(log, "SFTP", source, "remoteDir", remoteDir, "文件总数", totalFiles);
            scanDirectory(source == null ? null : source.sourceId(), channelSftp, config, remoteDir, cursor, seenCursor, contexts);
        } catch (Exception e) {
            throw new IllegalStateException("收取 SFTP 文件失败，remoteDir=" + config.remoteDir(), e);
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return contexts;
    }

    private ChannelSftp openSftpChannel(Session session, int timeoutMillis) throws JSchException {
        Channel channel = session.openChannel("sftp");
        channel.connect(timeoutMillis);
        return (ChannelSftp) channel;
    }

    private void scanDirectory(String sourceId, ChannelSftp channelSftp, SftpSourceConfig config, String remoteDir, String cursor, AtomicBoolean seenCursor, List<RecognitionContext> contexts)
            throws SftpException, IOException {
        if (!contexts.isEmpty() && config.limit() > 0 && contexts.size() >= config.limit()) {
            return;
        }
        Vector<LsEntry> entries = channelSftp.ls(remoteDir);
        List<LsEntry> sortedEntries = entries.stream()
                .sorted(Comparator.comparing(LsEntry::getFilename))
                .toList();
        for (LsEntry entry : sortedEntries) {
            if (shouldStop(sourceId)) {
                return;
            }
            if (contexts.size() >= config.limit() && config.limit() > 0) {
                return;
            }
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            if (!config.includeHidden() && name.startsWith(".")) {
                continue;
            }

            String childRemotePath = joinRemotePath(remoteDir, name);
            SftpATTRS attrs = entry.getAttrs();
            try {
                if (attrs.isDir()) {
                    if (config.recursive()) {
                        scanDirectory(sourceId, channelSftp, config, childRemotePath, cursor, seenCursor, contexts);
                    }
                    continue;
                }
                String checkpointKey = buildCheckpointKey(childRemotePath, attrs.getSize(), attrs.getMTime());
                if (cursor != null && !cursor.isBlank() && !seenCursor.get()) {
                    if (cursor.equals(checkpointKey)) {
                        seenCursor.set(true);
                    }
                    continue;
                }
                if (sourceId != null && transferSourceCheckpointGateway.existsProcessedItem(sourceId, checkpointKey)) {
                    continue;
                }
                Path tempFile = Files.createTempFile("transfer-sftp-", buildTempSuffix(name));
                try (InputStream inputStream = channelSftp.get(childRemotePath)) {
                    Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put(TransferConfigKeys.REMOTE_PATH, childRemotePath);
                attributes.put(TransferConfigKeys.CHECKPOINT_KEY, checkpointKey);
                attributes.put(TransferConfigKeys.CHECKPOINT_REF, childRemotePath);
                attributes.put(TransferConfigKeys.CHECKPOINT_NAME, name);
                attributes.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, checkpointKey);
                attributes.put("lastModified", attrs.getMTime() == 0 ? null : attrs.getMTime() * 1000L);
                attributes.put("permissions", attrs.getPermissionsString());
                attributes.put("tempPath", tempFile.toAbsolutePath().toString());
                contexts.add(new RecognitionContext(
                        SourceType.SFTP,
                        config.sourceCode(),
                        name,
                        null,
                        attrs.getSize(),
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
                        attributes
                ));
            } catch (Exception ex) {
                throw new IllegalStateException("读取 SFTP 文件失败，path=" + childRemotePath, ex);
            }
        }
    }

    private long countFiles(ChannelSftp channelSftp, SftpSourceConfig config, String remoteDir) throws SftpException {
        long total = 0L;
        Vector<LsEntry> entries = channelSftp.ls(remoteDir);
        for (LsEntry entry : entries.stream().sorted(Comparator.comparing(LsEntry::getFilename)).toList()) {
            String name = entry.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            if (!config.includeHidden() && name.startsWith(".")) {
                continue;
            }
            String childRemotePath = joinRemotePath(remoteDir, name);
            SftpATTRS attrs = entry.getAttrs();
            if (attrs.isDir()) {
                if (config.recursive()) {
                    total += countFiles(channelSftp, config, childRemotePath);
                }
                continue;
            }
            total++;
        }
        return total;
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

    private String normalizeRemoteDir(String remoteDir) {
        if (remoteDir == null || remoteDir.isBlank()) {
            throw new IllegalArgumentException("SFTP 来源缺少 remoteDir 配置");
        }
        return remoteDir.startsWith("/") ? remoteDir : "/" + remoteDir;
    }

    private String joinRemotePath(String parent, String child) {
        if (parent.endsWith("/")) {
            return parent + child;
        }
        return parent + "/" + child;
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

    private String buildCheckpointKey(String remotePath, long size, int mTime) {
        return String.join("|", remotePath, String.valueOf(size), String.valueOf(mTime));
    }

    private boolean shouldStop(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return false;
        }
        return transferSourceGateway.findById(sourceId)
                .map(current -> "STOPPING".equalsIgnoreCase(current.ingestStatus())
                        || "STOPPED".equalsIgnoreCase(current.ingestStatus()))
                .orElse(false);
    }

}
