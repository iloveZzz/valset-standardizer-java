package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 规则匹配结果。
 */
public class MatchResult {

    private final boolean matched;
    private final List<TransferRoute> routes;
    private final String reason;

    public MatchResult(boolean matched, List<TransferRoute> routes, String reason) {
        this.matched = matched;
        this.routes = routes;
        this.reason = reason;
    }



    public boolean matched() {
        return matched;
    }

    public List<TransferRoute> routes() {
        return routes;
    }

    public String reason() {
        return reason;
    }



    public boolean getMatched() {
        return matched;
    }

    public List<TransferRoute> getRoutes() {
        return routes;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatchResult other = (MatchResult) o;
        if (matched != other.matched) {
            return false;
        }
        if (!java.util.Objects.equals(routes, other.routes)) {
            return false;
        }
        if (!java.util.Objects.equals(reason, other.reason)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(matched, routes, reason);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MatchResult[");
        sb.append("matched=").append(matched);
        sb.append(", routes=").append(routes);
        sb.append(", reason=").append(reason);
        sb.append(']');
        return sb.toString();
    }



}
