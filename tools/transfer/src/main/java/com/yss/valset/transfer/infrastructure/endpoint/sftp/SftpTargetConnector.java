package com.yss.valset.transfer.infrastructure.endpoint.sftp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.SftpTargetConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SFTP 投递连接器。
 */
@Component
public class SftpTargetConnector implements TargetConnector {

    @Override
    public String type() {
        return TargetType.SFTP.name();
    }

    @Override
    public boolean supports(TransferTarget target) {
        return target != null && target.targetType() == TargetType.SFTP;
    }

    @Override
    public TransferResult send(TransferContext context) {
        SftpTargetConfig config = SftpTargetConfig.from(context);
        TransferObject transferObject = context.transferObject();
        File file = new File(transferObject.localTempPath());
        if (!file.isFile()) {
            return new TransferResult(false, null, Collections.singletonList("未找到待投递文件: " + transferObject.localTempPath()));
        }

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

            Channel channel = session.openChannel("sftp");
            channel.connect(config.channelTimeoutMillis());
            channelSftp = (ChannelSftp) channel;

            String remoteFilePath = buildRemoteFilePath(config, context, transferObject);
            ensureParentDirectory(channelSftp, remoteFilePath);
            try (FileInputStream inputStream = new FileInputStream(file)) {
                channelSftp.put(inputStream, remoteFilePath);
            }

            List<String> messages = new ArrayList<>();
            messages.add("SFTP 投递成功");
            messages.add("remotePath=" + remoteFilePath);
            return new TransferResult(true, null, remoteFilePath, messages);
        } catch (Exception e) {
            throw new IllegalStateException("SFTP 投递失败，remoteDir=" + config.remoteDir(), e);
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    String buildRemoteFilePath(SftpTargetConfig config, TransferContext context, TransferObject transferObject) {
        String remoteDir = firstNonBlank(
                stringValue(context.transferRoute().routeMeta(), TransferConfigKeys.TARGET_PATH, null),
                stringValue(context.transferRoute().routeMeta(), TransferConfigKeys.REMOTE_DIR, null),
                stringValue(context.transferTarget() == null ? null : context.transferTarget().targetMeta(), TransferConfigKeys.TARGET_PATH, null),
                stringValue(context.transferTarget() == null ? null : context.transferTarget().targetMeta(), TransferConfigKeys.REMOTE_DIR, null),
                context.transferTarget() == null ? null : context.transferTarget().targetPathTemplate(),
                config.remoteDir()
        );
        String fileName = firstNonBlank(transferObject.originalName(), "transfer-file");
        String resolvedRemoteDir = resolveTemplate(remoteDir, context, transferObject);
        if (resolvedRemoteDir == null || resolvedRemoteDir.isBlank()) {
            return fileName;
        }
        return joinRemotePath(resolvedRemoteDir, fileName);
    }

    private void ensureParentDirectory(ChannelSftp channelSftp, String remoteFilePath) throws Exception {
        int slashIndex = remoteFilePath.lastIndexOf('/');
        if (slashIndex <= 0) {
            return;
        }
        String parent = remoteFilePath.substring(0, slashIndex);
        createDirectories(channelSftp, parent);
    }

    private void createDirectories(ChannelSftp channelSftp, String remoteDir) throws Exception {
        if (remoteDir == null || remoteDir.isBlank() || "/".equals(remoteDir)) {
            return;
        }
        String normalized = remoteDir.startsWith("/") ? remoteDir : "/" + remoteDir;
        String[] segments = normalized.split("/");
        StringBuilder current = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            current.append('/').append(segment);
            try {
                channelSftp.cd(current.toString());
            } catch (Exception ex) {
                try {
                    channelSftp.mkdir(current.toString());
                } catch (Exception mkdirEx) {
                    if (!directoryExists(channelSftp, current.toString())) {
                        throw mkdirEx;
                    }
                }
            }
        }
    }

    private boolean directoryExists(ChannelSftp channelSftp, String remoteDir) {
        try {
            channelSftp.cd(remoteDir);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String joinRemotePath(String remoteDir, String fileName) {
        if (remoteDir == null || remoteDir.isBlank()) {
            return fileName;
        }
        String normalizedRemoteDir = remoteDir.endsWith("/") && remoteDir.length() > 1
                ? remoteDir.substring(0, remoteDir.length() - 1)
                : remoteDir;
        if (normalizedRemoteDir.endsWith("/")) {
            return normalizedRemoteDir + fileName;
        }
        return normalizedRemoteDir + "/" + fileName;
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config == null ? null : config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

}
