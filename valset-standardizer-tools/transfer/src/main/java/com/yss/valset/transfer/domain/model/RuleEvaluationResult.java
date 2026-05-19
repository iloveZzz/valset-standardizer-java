package com.yss.valset.transfer.domain.model;

import java.util.List;
import java.util.Map;

/**
 * 规则执行结果。
 */
public class RuleEvaluationResult {

    private final boolean matched;
    private final List<TransferRoute> routes;
    private final String message;
    private final Map<String, Object> result;

    public RuleEvaluationResult(boolean matched, List<TransferRoute> routes, String message) {
        this(matched, routes, message, java.util.Collections.emptyMap());
    }

    public RuleEvaluationResult(boolean matched, List<TransferRoute> routes, String message, Map<String, Object> result) {
        this.matched = matched;
        this.routes = routes;
        this.message = message;
        this.result = result == null ? java.util.Collections.emptyMap() : result;
    }



    public boolean matched() {
        return matched;
    }

    public List<TransferRoute> routes() {
        return routes;
    }

    public String message() {
        return message;
    }

    public Map<String, Object> result() {
        return result;
    }



    public boolean getMatched() {
        return matched;
    }

    public List<TransferRoute> getRoutes() {
        return routes;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuleEvaluationResult other = (RuleEvaluationResult) o;
        if (matched != other.matched) {
            return false;
        }
        if (!java.util.Objects.equals(routes, other.routes)) {
            return false;
        }
        if (!java.util.Objects.equals(message, other.message)) {
            return false;
        }
        if (!java.util.Objects.equals(result, other.result)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(matched, routes, message, result);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RuleEvaluationResult[");
        sb.append("matched=").append(matched);
        sb.append(", routes=").append(routes);
        sb.append(", message=").append(message);
        sb.append(", result=").append(result);
        sb.append(']');
        return sb.toString();
    }



}
