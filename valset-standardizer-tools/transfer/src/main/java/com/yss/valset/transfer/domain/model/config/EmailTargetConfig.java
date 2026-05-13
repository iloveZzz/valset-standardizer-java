package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 邮件投递配置。
 */
public class EmailTargetConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String protocol;
    private final boolean auth;
    private final boolean startTls;
    private final boolean ssl;
    private final int timeoutMillis;
    private final String from;
    private final String to;
    private final String cc;
    private final String bcc;
    private final String subjectTemplate;
    private final String bodyTemplate;
    private final boolean forwardMailContent;
    private final boolean forwardOriginalSender;

    public EmailTargetConfig(String host, int port, String username, String password, String protocol, boolean auth, boolean startTls, boolean ssl, int timeoutMillis, String from, String to, String cc, String bcc, String subjectTemplate, String bodyTemplate, boolean forwardMailContent, boolean forwardOriginalSender) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.protocol = protocol;
        this.auth = auth;
        this.startTls = startTls;
        this.ssl = ssl;
        this.timeoutMillis = timeoutMillis;
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.forwardMailContent = forwardMailContent;
        this.forwardOriginalSender = forwardOriginalSender;
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

    public String protocol() {
        return protocol;
    }

    public boolean auth() {
        return auth;
    }

    public boolean startTls() {
        return startTls;
    }

    public boolean ssl() {
        return ssl;
    }

    public int timeoutMillis() {
        return timeoutMillis;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String cc() {
        return cc;
    }

    public String bcc() {
        return bcc;
    }

    public String subjectTemplate() {
        return subjectTemplate;
    }

    public String bodyTemplate() {
        return bodyTemplate;
    }

    public boolean forwardMailContent() {
        return forwardMailContent;
    }

    public boolean forwardOriginalSender() {
        return forwardOriginalSender;
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

    public String getProtocol() {
        return protocol;
    }

    public boolean getAuth() {
        return auth;
    }

    public boolean getStartTls() {
        return startTls;
    }

    public boolean getSsl() {
        return ssl;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getCc() {
        return cc;
    }

    public String getBcc() {
        return bcc;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public boolean getForwardMailContent() {
        return forwardMailContent;
    }

    public boolean getForwardOriginalSender() {
        return forwardOriginalSender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailTargetConfig other = (EmailTargetConfig) o;
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
        if (!java.util.Objects.equals(protocol, other.protocol)) {
            return false;
        }
        if (auth != other.auth) {
            return false;
        }
        if (startTls != other.startTls) {
            return false;
        }
        if (ssl != other.ssl) {
            return false;
        }
        if (timeoutMillis != other.timeoutMillis) {
            return false;
        }
        if (!java.util.Objects.equals(from, other.from)) {
            return false;
        }
        if (!java.util.Objects.equals(to, other.to)) {
            return false;
        }
        if (!java.util.Objects.equals(cc, other.cc)) {
            return false;
        }
        if (!java.util.Objects.equals(bcc, other.bcc)) {
            return false;
        }
        if (!java.util.Objects.equals(subjectTemplate, other.subjectTemplate)) {
            return false;
        }
        if (!java.util.Objects.equals(bodyTemplate, other.bodyTemplate)) {
            return false;
        }
        if (forwardMailContent != other.forwardMailContent) {
            return false;
        }
        if (forwardOriginalSender != other.forwardOriginalSender) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(host, port, username, password, protocol, auth, startTls, ssl, timeoutMillis, from, to, cc, bcc, subjectTemplate, bodyTemplate, forwardMailContent, forwardOriginalSender);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EmailTargetConfig[");
        sb.append("host=").append(host);
        sb.append(", port=").append(port);
        sb.append(", username=").append(username);
        sb.append(", password=").append(password);
        sb.append(", protocol=").append(protocol);
        sb.append(", auth=").append(auth);
        sb.append(", startTls=").append(startTls);
        sb.append(", ssl=").append(ssl);
        sb.append(", timeoutMillis=").append(timeoutMillis);
        sb.append(", from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", cc=").append(cc);
        sb.append(", bcc=").append(bcc);
        sb.append(", subjectTemplate=").append(subjectTemplate);
        sb.append(", bodyTemplate=").append(bodyTemplate);
        sb.append(", forwardMailContent=").append(forwardMailContent);
        sb.append(", forwardOriginalSender=").append(forwardOriginalSender);
        sb.append(']');
        return sb.toString();
    }



public static EmailTargetConfig from(TransferContext context) {
        Map<String, Object> config = merge(context);
        String host = requiredString(config, TransferConfigKeys.HOST);
        int port = intValue(config, TransferConfigKeys.PORT, 25);
        String username = stringValue(config, TransferConfigKeys.USERNAME, null);
        String password = stringValue(config, TransferConfigKeys.PASSWORD, null);
        String protocol = stringValue(config, TransferConfigKeys.PROTOCOL, "smtp");
        boolean auth = booleanValue(config, TransferConfigKeys.AUTH, true);
        boolean startTls = booleanValue(config, TransferConfigKeys.START_TLS, true);
        boolean ssl = booleanValue(config, TransferConfigKeys.SSL, false);
        int timeoutMillis = intValue(config, TransferConfigKeys.TIMEOUT_MILLIS, 10000);
        String from = requiredString(config, TransferConfigKeys.FROM);
        String to = requiredString(config, TransferConfigKeys.TO);
        String cc = stringValue(config, TransferConfigKeys.CC, "");
        String bcc = stringValue(config, TransferConfigKeys.BCC, "");
        String subjectTemplate = stringValue(config, TransferConfigKeys.SUBJECT_TEMPLATE, null);
        String bodyTemplate = stringValue(config, TransferConfigKeys.BODY_TEMPLATE, null);
        boolean forwardMailContent = booleanValue(config, TransferConfigKeys.FORWARD_MAIL_CONTENT, true);
        boolean forwardOriginalSender = booleanValue(config, TransferConfigKeys.FORWARD_ORIGINAL_SENDER, true);
        return new EmailTargetConfig(host, port, username, password, protocol, auth, startTls, ssl, timeoutMillis, from, to, cc, bcc, subjectTemplate, bodyTemplate, forwardMailContent, forwardOriginalSender);
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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            throw new IllegalArgumentException("邮件目标缺少必要配置: " + key);
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
