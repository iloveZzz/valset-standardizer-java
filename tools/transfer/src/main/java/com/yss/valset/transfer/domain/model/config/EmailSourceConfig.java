package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Map;

/**
 * 邮件来源配置。
 */
public record EmailSourceConfig(
        String protocol,
        String host,
        int port,
        String username,
        String password,
        String folder,
        boolean ssl,
        boolean startTls,
        int limit,
        int timeoutMillis,
        String sourceCode
) {

    public static EmailSourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? Map.of() : source.connectionConfig();
        String protocol = normalizeProtocol(stringValue(config, TransferConfigKeys.PROTOCOL, "imap"));
        String host = requiredString(config, TransferConfigKeys.HOST);
        int port = intValue(config, TransferConfigKeys.PORT, defaultPort(protocol));
        String username = requiredString(config, TransferConfigKeys.USERNAME);
        String password = requiredString(config, TransferConfigKeys.PASSWORD);
        String folder = stringValue(config, TransferConfigKeys.FOLDER, "INBOX");
        boolean ssl = booleanValue(config, TransferConfigKeys.SSL, protocol.endsWith("s"));
        boolean startTls = booleanValue(config, TransferConfigKeys.START_TLS, false);
        int limit = intValue(config, TransferConfigKeys.LIMIT, 0);
        int timeoutMillis = intValue(config, TransferConfigKeys.TIMEOUT_MILLIS, 30000);
        String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank() ? username : source.sourceCode();
        return new EmailSourceConfig(protocol, host, port, username, password, folder, ssl, startTls, limit, timeoutMillis, sourceCode);
    }

    public int effectiveLimit() {
        return limit <= 0 ? 50 : Math.min(limit, 50);
    }

    private static String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            return "imap";
        }
        return switch (protocol.toLowerCase()) {
            case "imap", "imaps", "pop3", "pop3s" -> protocol.toLowerCase();
            default -> throw new IllegalArgumentException("不支持的邮件协议: " + protocol);
        };
    }

    private static int defaultPort(String protocol) {
        return switch (protocol.toLowerCase()) {
            case "imaps", "pop3s" -> 993;
            case "pop3" -> 110;
            default -> 143;
        };
    }

    private static String requiredString(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException("邮件来源缺少必要配置: " + key);
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
