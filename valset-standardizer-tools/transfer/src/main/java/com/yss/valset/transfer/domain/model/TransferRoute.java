package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 文件路由结果。
 */
public class TransferRoute {

    private final String routeId;
    private final String sourceId;
    private final SourceType sourceType;
    private final String sourceCode;
    private final String ruleId;
    private final TargetType targetType;
    private final String targetCode;
    private final String pollCron;
    private final String targetPath;
    private final String renamePattern;
    private final boolean enabled;
    private final TransferStatus routeStatus;
    private final Map<String, Object> routeMeta;

    public TransferRoute(String routeId, String sourceId, SourceType sourceType, String sourceCode, String ruleId, TargetType targetType, String targetCode, String pollCron, String targetPath, String renamePattern, boolean enabled, TransferStatus routeStatus, Map<String, Object> routeMeta) {
        this.routeId = routeId;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.sourceCode = sourceCode;
        this.ruleId = ruleId;
        this.targetType = targetType;
        this.targetCode = targetCode;
        this.pollCron = pollCron;
        this.targetPath = targetPath;
        this.renamePattern = renamePattern;
        this.enabled = enabled;
        this.routeStatus = routeStatus;
        this.routeMeta = routeMeta;
    }



    public String routeId() {
        return routeId;
    }

    public String sourceId() {
        return sourceId;
    }

    public SourceType sourceType() {
        return sourceType;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String ruleId() {
        return ruleId;
    }

    public TargetType targetType() {
        return targetType;
    }

    public String targetCode() {
        return targetCode;
    }

    public String pollCron() {
        return pollCron;
    }

    public String targetPath() {
        return targetPath;
    }

    public String renamePattern() {
        return renamePattern;
    }

    public boolean enabled() {
        return enabled;
    }

    public TransferStatus routeStatus() {
        return routeStatus;
    }

    public Map<String, Object> routeMeta() {
        return routeMeta;
    }



    public String getRouteId() {
        return routeId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getRuleId() {
        return ruleId;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public String getPollCron() {
        return pollCron;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getRenamePattern() {
        return renamePattern;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public TransferStatus getRouteStatus() {
        return routeStatus;
    }

    public Map<String, Object> getRouteMeta() {
        return routeMeta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRoute other = (TransferRoute) o;
        if (!java.util.Objects.equals(routeId, other.routeId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        if (!java.util.Objects.equals(ruleId, other.ruleId)) {
            return false;
        }
        if (!java.util.Objects.equals(targetType, other.targetType)) {
            return false;
        }
        if (!java.util.Objects.equals(targetCode, other.targetCode)) {
            return false;
        }
        if (!java.util.Objects.equals(pollCron, other.pollCron)) {
            return false;
        }
        if (!java.util.Objects.equals(targetPath, other.targetPath)) {
            return false;
        }
        if (!java.util.Objects.equals(renamePattern, other.renamePattern)) {
            return false;
        }
        if (enabled != other.enabled) {
            return false;
        }
        if (!java.util.Objects.equals(routeStatus, other.routeStatus)) {
            return false;
        }
        if (!java.util.Objects.equals(routeMeta, other.routeMeta)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(routeId, sourceId, sourceType, sourceCode, ruleId, targetType, targetCode, pollCron, targetPath, renamePattern, enabled, routeStatus, routeMeta);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRoute[");
        sb.append("routeId=").append(routeId);
        sb.append(", sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", ruleId=").append(ruleId);
        sb.append(", targetType=").append(targetType);
        sb.append(", targetCode=").append(targetCode);
        sb.append(", pollCron=").append(pollCron);
        sb.append(", targetPath=").append(targetPath);
        sb.append(", renamePattern=").append(renamePattern);
        sb.append(", enabled=").append(enabled);
        sb.append(", routeStatus=").append(routeStatus);
        sb.append(", routeMeta=").append(routeMeta);
        sb.append(']');
        return sb.toString();
    }



}
