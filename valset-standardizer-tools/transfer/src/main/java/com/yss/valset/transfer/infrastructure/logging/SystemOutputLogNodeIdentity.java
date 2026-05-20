package com.yss.valset.transfer.infrastructure.logging;

import com.yss.valset.transfer.application.dto.SystemOutputLogNodeDTO;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 当前 JVM 日志节点标识。
 */
public final class SystemOutputLogNodeIdentity {

    private static final String DEFAULT_SERVICE_NAME = "valset-standardizer";

    private SystemOutputLogNodeIdentity() {
    }

    public static SystemOutputLogNodeDTO current(String serviceName, String serverPort) {
        String normalizedServiceName = valueOrDefault(serviceName, DEFAULT_SERVICE_NAME);
        String host = resolveHost();
        String port = valueOrDefault(serverPort, "-");
        String pid = resolvePid();
        String nodeId = normalizedServiceName + "@" + host + ":" + port + "#" + pid;
        SystemOutputLogNodeDTO node = new SystemOutputLogNodeDTO();
        node.setNodeId(nodeId);
        node.setNodeName(normalizedServiceName + " / " + host + ":" + port + " / PID " + pid);
        node.setServiceName(normalizedServiceName);
        node.setHost(host);
        node.setPort(port);
        node.setPid(pid);
        node.setCurrent(true);
        return node;
    }

    private static String resolveHost() {
        String hostname = System.getenv("HOSTNAME");
        if (hasText(hostname)) {
            return hostname.trim();
        }
        String computerName = System.getenv("COMPUTERNAME");
        if (hasText(computerName)) {
            return computerName.trim();
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "unknown-host";
        }
    }

    private static String resolvePid() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (hasText(runtimeName)) {
            int separatorIndex = runtimeName.indexOf('@');
            if (separatorIndex > 0) {
                return runtimeName.substring(0, separatorIndex);
            }
            return runtimeName;
        }
        return "-";
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (!hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
