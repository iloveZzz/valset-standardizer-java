package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SFTP 投递配置。
 */
public record SftpTargetConfig(
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
        int channelTimeoutMillis
) {

    public static SftpTargetConfig from(TransferContext context) {
        Map<String, Object> config = merge(context);
        String host = requiredString(config, TransferConfigKeys.HOST);
        int port = intValue(config, TransferConfigKeys.PORT, 22);
        String username = requiredString(config, TransferConfigKeys.USERNAME);
        String password = stringValue(config, TransferConfigKeys.PASSWORD, null);
        String privateKeyPath = stringValue(config, TransferConfigKeys.PRIVATE_KEY_PATH, null);
        String passphrase = stringValue(config, TransferConfigKeys.PASSPHRASE, null);
        String remoteDir = stringValue(config, TransferConfigKeys.REMOTE_DIR, null);
        boolean recursive = booleanValue(config, TransferConfigKeys.RECURSIVE, false);
        boolean includeHidden = booleanValue(config, TransferConfigKeys.INCLUDE_HIDDEN, false);
        int limit = intValue(config, TransferConfigKeys.LIMIT, 0);
        boolean strictHostKeyChecking = booleanValue(config, TransferConfigKeys.STRICT_HOST_KEY_CHECKING, false);
        int connectTimeoutMillis = intValue(config, TransferConfigKeys.CONNECT_TIMEOUT_MILLIS, 10000);
        int channelTimeoutMillis = intValue(config, TransferConfigKeys.CHANNEL_TIMEOUT_MILLIS, 10000);
        return new SftpTargetConfig(host, port, username, password, privateKeyPath, passphrase, remoteDir, recursive, includeHidden, limit, strictHostKeyChecking, connectTimeoutMillis, channelTimeoutMillis);
    }

    private static Map<String, Object> merge(TransferContext context) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (context.transferObject() != null && context.transferObject().fileMeta() != null) {
            config.putAll(context.transferObject().fileMeta());
        }
        if (context.attributes() != null) {
            config.putAll(context.attributes());
        }
        if (context.transferRoute() != null && context.transferRoute().routeMeta() != null) {
            config.putAll(context.transferRoute().routeMeta());
        }
        if (context.transferTarget() != null) {
            if (context.transferTarget().connectionConfig() != null) {
                config.putAll(context.transferTarget().connectionConfig());
            }
            if (context.transferTarget().targetMeta() != null) {
                config.putAll(context.transferTarget().targetMeta());
            }
        }
        return config;
    }

    private static String requiredString(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException("SFTP 目标缺少必要配置: " + key);
        }
        return String.valueOf(raw);
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }
}
