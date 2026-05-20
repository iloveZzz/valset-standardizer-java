package com.yss.valset.transfer.infrastructure.logging;

import com.yss.valset.transfer.application.dto.SystemOutputLogCleanupResponse;
import com.yss.valset.transfer.application.dto.SystemOutputLogNodeDTO;
import com.yss.valset.transfer.application.dto.SystemOutputLogViewDTO;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统输出日志内存环形缓冲区。
 */
public final class InMemorySystemOutputLogStore {

    public static final int DEFAULT_MAX_LINES = 10_000;

    private static final InMemorySystemOutputLogStore INSTANCE = new InMemorySystemOutputLogStore();

    private final Object monitor = new Object();

    private final Deque<SystemOutputLogViewDTO> lines = new ArrayDeque<>(DEFAULT_MAX_LINES);

    private final AtomicLong sequenceGenerator = new AtomicLong(0L);

    private volatile int maxLines = DEFAULT_MAX_LINES;

    private volatile SystemOutputLogNodeDTO currentNode = SystemOutputLogNodeIdentity.current(null, null);

    private InMemorySystemOutputLogStore() {
    }

    public static InMemorySystemOutputLogStore getInstance() {
        return INSTANCE;
    }

    public void configureMaxLines(int configuredMaxLines) {
        if (configuredMaxLines <= 0) {
            return;
        }
        synchronized (monitor) {
            maxLines = configuredMaxLines;
            trimOverflow();
        }
    }

    public void configureCurrentNode(SystemOutputLogNodeDTO node) {
        if (node == null || !hasText(node.getNodeId())) {
            return;
        }
        currentNode = node;
    }

    public SystemOutputLogNodeDTO currentNode() {
        return currentNode;
    }

    public List<SystemOutputLogNodeDTO> nodes() {
        synchronized (monitor) {
            Map<String, SystemOutputLogNodeDTO> nodeMap = new LinkedHashMap<>();
            SystemOutputLogNodeDTO current = currentNode;
            if (current != null && hasText(current.getNodeId())) {
                nodeMap.put(current.getNodeId(), current);
            }
            for (SystemOutputLogViewDTO line : lines) {
                if (line == null || !hasText(line.getNodeId())) {
                    continue;
                }
                SystemOutputLogNodeDTO node = new SystemOutputLogNodeDTO();
                node.setNodeId(line.getNodeId());
                node.setNodeName(line.getNodeName());
                node.setServiceName(line.getServiceName());
                node.setCurrent(current != null && line.getNodeId().equals(current.getNodeId()));
                nodeMap.put(line.getNodeId(), node);
            }
            return new ArrayList<>(nodeMap.values());
        }
    }

    public void append(String timestamp,
                       String nodeId,
                       String nodeName,
                       String serviceName,
                       String level,
                       String threadName,
                       String loggerName,
                       String message,
                       String formattedLine) {
        SystemOutputLogViewDTO line = new SystemOutputLogViewDTO();
        line.setSequence(sequenceGenerator.incrementAndGet());
        line.setTimestamp(timestamp);
        line.setNodeId(nodeId);
        line.setNodeName(nodeName);
        line.setServiceName(serviceName);
        line.setLevel(level);
        line.setThreadName(threadName);
        line.setLoggerName(loggerName);
        line.setMessage(message);
        line.setFormattedLine(formattedLine);
        synchronized (monitor) {
            lines.addLast(line);
            trimOverflow();
        }
    }

    public List<SystemOutputLogViewDTO> latest(String nodeId, Integer limit) {
        int maxSize = normalizeLimit(limit);
        synchronized (monitor) {
            if (lines.isEmpty()) {
                return Collections.emptyList();
            }
            List<SystemOutputLogViewDTO> matchedLines = new ArrayList<>(Math.min(lines.size(), maxSize));
            for (SystemOutputLogViewDTO line : lines) {
                if (!matchesNode(line, nodeId)) {
                    continue;
                }
                matchedLines.add(line);
                if (matchedLines.size() > maxSize) {
                    matchedLines.remove(0);
                }
            }
            return matchedLines;
        }
    }

    public List<SystemOutputLogViewDTO> after(String nodeId, long sequence, Integer limit) {
        int maxSize = normalizeLimit(limit);
        synchronized (monitor) {
            if (lines.isEmpty()) {
                return Collections.emptyList();
            }
            List<SystemOutputLogViewDTO> snapshot = new ArrayList<>(Math.min(lines.size(), maxSize));
            for (SystemOutputLogViewDTO line : lines) {
                if (!matchesNode(line, nodeId)) {
                    continue;
                }
                if (line.getSequence() <= sequence) {
                    continue;
                }
                snapshot.add(line);
                if (snapshot.size() >= maxSize) {
                    break;
                }
            }
            return snapshot;
        }
    }

    public SystemOutputLogCleanupResponse clear(String nodeId) {
        synchronized (monitor) {
            if (!hasText(nodeId)) {
                long deletedCount = lines.size();
                lines.clear();
                return new SystemOutputLogCleanupResponse(deletedCount, 0L);
            }
            long beforeCount = lines.size();
            lines.removeIf(line -> matchesNode(line, nodeId));
            long deletedCount = beforeCount - lines.size();
            long remainingCount = 0L;
            for (SystemOutputLogViewDTO line : lines) {
                if (matchesNode(line, nodeId)) {
                    remainingCount++;
                }
            }
            return new SystemOutputLogCleanupResponse(deletedCount, remainingCount);
        }
    }

    private void trimOverflow() {
        while (lines.size() > maxLines) {
            lines.removeFirst();
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return maxLines;
        }
        return Math.min(limit, maxLines);
    }

    private boolean matchesNode(SystemOutputLogViewDTO line, String nodeId) {
        if (!hasText(nodeId)) {
            return true;
        }
        return line != null && nodeId.trim().equals(line.getNodeId());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
