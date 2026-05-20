package com.yss.valset.transfer.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.yss.valset.transfer.application.dto.SystemOutputLogNodeDTO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 收集 Logback 系统输出日志到内存，供前端运行日志页实时查看。
 */
public class SystemOutputLogAppender extends AppenderBase<ILoggingEvent> {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private int maxLines = InMemorySystemOutputLogStore.DEFAULT_MAX_LINES;

    private String serviceName;

    private String serverPort;

    private SystemOutputLogNodeDTO node;

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void start() {
        node = SystemOutputLogNodeIdentity.current(serviceName, serverPort);
        InMemorySystemOutputLogStore.getInstance().configureMaxLines(maxLines);
        InMemorySystemOutputLogStore.getInstance().configureCurrentNode(node);
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null) {
            return;
        }
        String timestamp = formatTimestamp(event.getTimeStamp());
        String level = event.getLevel() == null ? "-" : event.getLevel().toString();
        String threadName = valueOrDash(event.getThreadName());
        String loggerName = valueOrDash(event.getLoggerName());
        appendLine(timestamp, level, threadName, loggerName, valueOrDash(event.getFormattedMessage()));
        appendThrowable(timestamp, level, threadName, loggerName, event.getThrowableProxy());
    }

    private void appendThrowable(String timestamp,
                                 String level,
                                 String threadName,
                                 String loggerName,
                                 IThrowableProxy throwableProxy) {
        IThrowableProxy current = throwableProxy;
        while (current != null) {
            appendLine(timestamp, level, threadName, loggerName, current.getClassName() + ": " + valueOrDash(current.getMessage()));
            StackTraceElementProxy[] stackTraceElements = current.getStackTraceElementProxyArray();
            if (stackTraceElements != null) {
                for (StackTraceElementProxy stackTraceElement : stackTraceElements) {
                    appendLine(timestamp, level, threadName, loggerName, "    at " + stackTraceElement.getSTEAsString());
                }
            }
            current = current.getCause();
            if (current != null) {
                appendLine(timestamp, level, threadName, loggerName, "Caused by:");
            }
        }
    }

    private void appendLine(String timestamp,
                            String level,
                            String threadName,
                            String loggerName,
                            String message) {
        String formattedLine = timestamp
                + " [" + threadName + "] "
                + padRight(level, 5)
                + " " + loggerName
                + " - " + message;
        SystemOutputLogNodeDTO currentNode = node == null ? InMemorySystemOutputLogStore.getInstance().currentNode() : node;
        InMemorySystemOutputLogStore.getInstance().append(
                timestamp,
                currentNode.getNodeId(),
                currentNode.getNodeName(),
                currentNode.getServiceName(),
                level,
                threadName,
                loggerName,
                message,
                formattedLine
        );
    }

    private String formatTimestamp(long timestamp) {
        return TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZONE_ID));
    }

    private String valueOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value;
    }

    private String padRight(String value, int length) {
        if (value == null) {
            value = "";
        }
        if (value.length() >= length) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < length) {
            builder.append(' ');
        }
        return builder.toString();
    }
}
