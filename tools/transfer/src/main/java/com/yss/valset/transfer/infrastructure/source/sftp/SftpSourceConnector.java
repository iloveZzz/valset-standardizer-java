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
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * SFTP 来源连接器，支持从远程目录拉取文件并转入临时文件。
 */
@Component
public class SftpSourceConnector implements SourceConnector {

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
        SftpConfig config = SftpConfig.from(source);
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
            scanDirectory(channelSftp, config, normalizeRemoteDir(config.remoteDir()), contexts);
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

    private void scanDirectory(ChannelSftp channelSftp, SftpConfig config, String remoteDir, List<RecognitionContext> contexts)
            throws SftpException, IOException {
        if (!contexts.isEmpty() && config.limit() > 0 && contexts.size() >= config.limit()) {
            return;
        }
        Vector<LsEntry> entries = channelSftp.ls(remoteDir);
        entries.stream()
                .sorted(Comparator.comparing(LsEntry::getFilename))
                .forEach(entry -> {
                    if (contexts.size() >= config.limit() && config.limit() > 0) {
                        return;
                    }
                    String name = entry.getFilename();
                    if (".".equals(name) || "..".equals(name)) {
                        return;
                    }
                    if (!config.includeHidden() && name.startsWith(".")) {
                        return;
                    }

                    String childRemotePath = joinRemotePath(remoteDir, name);
                    SftpATTRS attrs = entry.getAttrs();
                    try {
                        if (attrs.isDir()) {
                            if (config.recursive()) {
                                scanDirectory(channelSftp, config, childRemotePath, contexts);
                            }
                            return;
                        }
                        Path tempFile = Files.createTempFile("transfer-sftp-", "-" + sanitizeFileName(name));
                        try (InputStream inputStream = channelSftp.get(childRemotePath)) {
                            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        Map<String, Object> attributes = new LinkedHashMap<>();
                        attributes.put("remotePath", childRemotePath);
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
                                tempFile.toAbsolutePath().toString(),
                                attributes
                        ));
                    } catch (Exception ex) {
                        throw new IllegalStateException("读取 SFTP 文件失败，path=" + childRemotePath, ex);
                    }
                });
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

    private record SftpConfig(
            String host,
            int port,
            String username,
            String password,
            String privateKeyPath,
            String passphrase,
            String remoteDir,
            boolean recursive,
            boolean includeHidden,
            int limit,
            boolean strictHostKeyChecking,
            int connectTimeoutMillis,
            int channelTimeoutMillis,
            String sourceCode
    ) {
        static SftpConfig from(TransferSource source) {
            Map<String, Object> config = source.connectionConfig() == null ? Collections.emptyMap() : source.connectionConfig();
            String host = requiredString(config, "host");
            int port = intValue(config, "port", 22);
            String username = requiredString(config, "username");
            String password = stringValue(config, "password", null);
            String privateKeyPath = stringValue(config, "privateKeyPath", null);
            String passphrase = stringValue(config, "passphrase", null);
            String remoteDir = stringValue(config, "remoteDir", null);
            boolean recursive = booleanValue(config, "recursive", false);
            boolean includeHidden = booleanValue(config, "includeHidden", false);
            int limit = intValue(config, "limit", 0);
            boolean strictHostKeyChecking = booleanValue(config, "strictHostKeyChecking", false);
            int connectTimeoutMillis = intValue(config, "connectTimeoutMillis", 10000);
            int channelTimeoutMillis = intValue(config, "channelTimeoutMillis", 10000);
            String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank() ? username : source.sourceCode();
            return new SftpConfig(
                    host,
                    port,
                    username,
                    password,
                    privateKeyPath,
                    passphrase,
                    remoteDir,
                    recursive,
                    includeHidden,
                    limit,
                    strictHostKeyChecking,
                    connectTimeoutMillis,
                    channelTimeoutMillis,
                    sourceCode
            );
        }

        private static String requiredString(Map<String, Object> config, String key) {
            Object raw = config.get(key);
            if (raw == null || String.valueOf(raw).isBlank()) {
                throw new IllegalArgumentException("SFTP 来源缺少必要配置: " + key);
            }
            return String.valueOf(raw);
        }

        private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
            Object raw = config.get(key);
            return raw == null ? defaultValue : String.valueOf(raw);
        }

        private static int intValue(Map<String, Object> config, String key, int defaultValue) {
            Object raw = config.get(key);
            if (raw == null) {
                return defaultValue;
            }
            return Integer.parseInt(String.valueOf(raw));
        }

        private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
            Object raw = config.get(key);
            if (raw == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(String.valueOf(raw));
        }
    }
}
