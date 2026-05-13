package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Collections;
import java.util.Map;

/**
 * SFTP 来源配置。
 */
public class SftpSourceConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String privateKeyPath;
    private final String passphrase;
    private final String remoteDir;
    private final boolean recursive;
    private final boolean includeHidden;
    private final int limit;
    private final boolean strictHostKeyChecking;
    private final int connectTimeoutMillis;
    private final int channelTimeoutMillis;
    private final String sourceCode;

    public SftpSourceConfig(String host, int port, String username, String password, String privateKeyPath, String passphrase, String remoteDir, boolean recursive, boolean includeHidden, int limit, boolean strictHostKeyChecking, int connectTimeoutMillis, int channelTimeoutMillis, String sourceCode) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
        this.remoteDir = remoteDir;
        this.recursive = recursive;
        this.includeHidden = includeHidden;
        this.limit = limit;
        this.strictHostKeyChecking = strictHostKeyChecking;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.channelTimeoutMillis = channelTimeoutMillis;
        this.sourceCode = sourceCode;
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

    public String privateKeyPath() {
        return privateKeyPath;
    }

    public String passphrase() {
        return passphrase;
    }

    public String remoteDir() {
        return remoteDir;
    }

    public boolean recursive() {
        return recursive;
    }

    public boolean includeHidden() {
        return includeHidden;
    }

    public int limit() {
        return limit;
    }

    public boolean strictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int channelTimeoutMillis() {
        return channelTimeoutMillis;
    }

    public String sourceCode() {
        return sourceCode;
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

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public String getRemoteDir() {
        return remoteDir;
    }

    public boolean getRecursive() {
        return recursive;
    }

    public boolean getIncludeHidden() {
        return includeHidden;
    }

    public int getLimit() {
        return limit;
    }

    public boolean getStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getChannelTimeoutMillis() {
        return channelTimeoutMillis;
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
        SftpSourceConfig other = (SftpSourceConfig) o;
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
        if (!java.util.Objects.equals(privateKeyPath, other.privateKeyPath)) {
            return false;
        }
        if (!java.util.Objects.equals(passphrase, other.passphrase)) {
            return false;
        }
        if (!java.util.Objects.equals(remoteDir, other.remoteDir)) {
            return false;
        }
        if (recursive != other.recursive) {
            return false;
        }
        if (includeHidden != other.includeHidden) {
            return false;
        }
        if (limit != other.limit) {
            return false;
        }
        if (strictHostKeyChecking != other.strictHostKeyChecking) {
            return false;
        }
        if (connectTimeoutMillis != other.connectTimeoutMillis) {
            return false;
        }
        if (channelTimeoutMillis != other.channelTimeoutMillis) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(host, port, username, password, privateKeyPath, passphrase, remoteDir, recursive, includeHidden, limit, strictHostKeyChecking, connectTimeoutMillis, channelTimeoutMillis, sourceCode);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SftpSourceConfig[");
        sb.append("host=").append(host);
        sb.append(", port=").append(port);
        sb.append(", username=").append(username);
        sb.append(", password=").append(password);
        sb.append(", privateKeyPath=").append(privateKeyPath);
        sb.append(", passphrase=").append(passphrase);
        sb.append(", remoteDir=").append(remoteDir);
        sb.append(", recursive=").append(recursive);
        sb.append(", includeHidden=").append(includeHidden);
        sb.append(", limit=").append(limit);
        sb.append(", strictHostKeyChecking=").append(strictHostKeyChecking);
        sb.append(", connectTimeoutMillis=").append(connectTimeoutMillis);
        sb.append(", channelTimeoutMillis=").append(channelTimeoutMillis);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(']');
        return sb.toString();
    }



public static SftpSourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? Collections.emptyMap() : source.connectionConfig();
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
        String sourceCode = source.sourceCode() == null || source.sourceCode().trim().isEmpty() ? username : source.sourceCode();
        return new SftpSourceConfig(
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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
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
