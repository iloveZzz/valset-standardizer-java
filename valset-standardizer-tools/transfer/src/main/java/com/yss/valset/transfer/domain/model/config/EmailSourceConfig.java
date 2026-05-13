package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Map;

/**
 * 邮件来源配置。
 */
public class EmailSourceConfig {

    private final String protocol;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String folder;
    private final int mailTimeRangeDays;
    private final int scanBatchSize;
    private final int pop3LargeMailboxThreshold;
    private final boolean ssl;
    private final boolean startTls;
    private final int limit;
    private final int timeoutMillis;
    private final String sourceCode;

    public EmailSourceConfig(String protocol, String host, int port, String username, String password, String folder, int mailTimeRangeDays, int scanBatchSize, int pop3LargeMailboxThreshold, boolean ssl, boolean startTls, int limit, int timeoutMillis, String sourceCode) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.folder = folder;
        this.mailTimeRangeDays = mailTimeRangeDays;
        this.scanBatchSize = scanBatchSize;
        this.pop3LargeMailboxThreshold = pop3LargeMailboxThreshold;
        this.ssl = ssl;
        this.startTls = startTls;
        this.limit = limit;
        this.timeoutMillis = timeoutMillis;
        this.sourceCode = sourceCode;
    }



    public String protocol() {
        return protocol;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String folder() {
        return folder;
    }

    public int mailTimeRangeDays() {
        return mailTimeRangeDays;
    }

    public int scanBatchSize() {
        return scanBatchSize;
    }

    public int pop3LargeMailboxThreshold() {
        return pop3LargeMailboxThreshold;
    }

    public boolean ssl() {
        return ssl;
    }

    public boolean startTls() {
        return startTls;
    }

    public int limit() {
        return limit;
    }

    public int timeoutMillis() {
        return timeoutMillis;
    }

    public String sourceCode() {
        return sourceCode;
    }



    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFolder() {
        return folder;
    }

    public int getMailTimeRangeDays() {
        return mailTimeRangeDays;
    }

    public int getScanBatchSize() {
        return scanBatchSize;
    }

    public int getPop3LargeMailboxThreshold() {
        return pop3LargeMailboxThreshold;
    }

    public boolean getSsl() {
        return ssl;
    }

    public boolean getStartTls() {
        return startTls;
    }

    public int getLimit() {
        return limit;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailSourceConfig other = (EmailSourceConfig) o;
        if (!java.util.Objects.equals(protocol, other.protocol)) {
            return false;
        }
        if (!java.util.Objects.equals(host, other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (!java.util.Objects.equals(username, other.username)) {
            return false;
        }
        if (!java.util.Objects.equals(password, other.password)) {
            return false;
        }
        if (!java.util.Objects.equals(folder, other.folder)) {
            return false;
        }
        if (mailTimeRangeDays != other.mailTimeRangeDays) {
            return false;
        }
        if (scanBatchSize != other.scanBatchSize) {
            return false;
        }
        if (pop3LargeMailboxThreshold != other.pop3LargeMailboxThreshold) {
            return false;
        }
        if (ssl != other.ssl) {
            return false;
        }
        if (startTls != other.startTls) {
            return false;
        }
        if (limit != other.limit) {
            return false;
        }
        if (timeoutMillis != other.timeoutMillis) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(protocol, host, port, username, password, folder, mailTimeRangeDays, scanBatchSize, pop3LargeMailboxThreshold, ssl, startTls, limit, timeoutMillis, sourceCode);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EmailSourceConfig[");
        sb.append("protocol=").append(protocol);
        sb.append(", host=").append(host);
        sb.append(", port=").append(port);
        sb.append(", username=").append(username);
        sb.append(", password=").append(password);
        sb.append(", folder=").append(folder);
        sb.append(", mailTimeRangeDays=").append(mailTimeRangeDays);
        sb.append(", scanBatchSize=").append(scanBatchSize);
        sb.append(", pop3LargeMailboxThreshold=").append(pop3LargeMailboxThreshold);
        sb.append(", ssl=").append(ssl);
        sb.append(", startTls=").append(startTls);
        sb.append(", limit=").append(limit);
        sb.append(", timeoutMillis=").append(timeoutMillis);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(']');
        return sb.toString();
    }



public static EmailSourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? java.util.Collections.emptyMap() : source.connectionConfig();
        String protocol = normalizeProtocol(stringValue(config, TransferConfigKeys.PROTOCOL, "imap"));
        String host = requiredString(config, TransferConfigKeys.HOST);
        int port = intValue(config, TransferConfigKeys.PORT, defaultPort(protocol));
        String username = requiredString(config, TransferConfigKeys.USERNAME);
        String password = requiredString(config, TransferConfigKeys.PASSWORD);
        String folder = stringValue(config, TransferConfigKeys.FOLDER, "INBOX");
        int mailTimeRangeDays = Math.max(0, intValue(config, TransferConfigKeys.MAIL_TIME_RANGE_DAYS, 0));
        int scanBatchSize = Math.max(1, intValue(config, TransferConfigKeys.MAIL_SCAN_BATCH_SIZE, 100));
        int pop3LargeMailboxThreshold = Math.max(0, intValue(config, TransferConfigKeys.POP3_LARGE_MAILBOX_THRESHOLD, 2000));
        boolean ssl = booleanValue(config, TransferConfigKeys.SSL, protocol.endsWith("s"));
        boolean startTls = booleanValue(config, TransferConfigKeys.START_TLS, false);
        int limit = intValue(config, TransferConfigKeys.LIMIT, 0);
        int timeoutMillis = intValue(config, TransferConfigKeys.TIMEOUT_MILLIS, 30000);
        String sourceCode = source.sourceCode() == null || source.sourceCode().trim().isEmpty() ? username : source.sourceCode();
        return new EmailSourceConfig(protocol, host, port, username, password, folder, mailTimeRangeDays, scanBatchSize, pop3LargeMailboxThreshold, ssl, startTls, limit, timeoutMillis, sourceCode);
    }

    public int effectiveLimit() {
        return limit <= 0 ? 50 : Math.min(limit, 50);
    }

    public int effectiveScanBatchSize() {
        return Math.min(Math.max(scanBatchSize, 1), 500);
    }

    public boolean shouldApplyMailTimeRange() {
        return mailTimeRangeDays > 0;
    }

    public boolean isPop3LargeMailbox(int messageCount) {
        return protocol != null
                && protocol.startsWith("pop3")
                && pop3LargeMailboxThreshold > 0
                && messageCount >= pop3LargeMailboxThreshold;
    }

    private static String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.trim().isEmpty()) {
            return "imap";
        }
        String normalized = protocol.toLowerCase();
        if ("imap".equals(normalized) || "imaps".equals(normalized) || "pop3".equals(normalized) || "pop3s".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("不支持的邮件协议: " + protocol);
    }

    private static int defaultPort(String protocol) {
        String normalized = protocol.toLowerCase();
        if ("imaps".equals(normalized)) {
            return 993;
        }
        if ("pop3s".equals(normalized)) {
            return 995;
        }
        if ("pop3".equals(normalized)) {
            return 110;
        }
        return 143;
    }

    private static String requiredString(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }

}
