package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 邮件投递配置。
 */
public record EmailTargetConfig(
        String host,
        int port,
        String username,
        String password,
        String protocol,
        boolean auth,
        boolean startTls,
        boolean ssl,
        int timeoutMillis,
        String from,
        String to,
        String cc,
        String bcc,
        String subjectTemplate,
        String bodyTemplate,
        boolean forwardMailContent,
        boolean forwardOriginalSender
) {

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
        if (raw == null || String.valueOf(raw).isBlank()) {
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
