package com.yss.valset.transfer.domain.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 条件规则类，用于表示和生成规则表达式或 SQL 条件语句。
 * 支持叶子节点和组合节点两种类型，组合节点可通过逻辑操作符(AND/OR)连接多个子规则。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionRule implements Serializable {
    private NodeType type;
    private LOGICAL_OP logicalOp;
    private String ruleId;
    private String description;
    private List<ConditionRule> children;

    private String field;
    private String operator;
    private Object value;
    private Object betweenValue1;
    private Object betweenValue2;

    @JsonCreator
    public static ConditionRule fromString(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, ConditionRule.class);
    }

    /**
     * 节点类型枚举。
     */
    public enum NodeType {
        LEAF,
        GROUP
    }

    /**
     * 逻辑操作符枚举。
     */
    public enum LOGICAL_OP {
        AND,
        OR
    }

    /**
     * 将当前规则转换为 QLExpress 规则表达式字符串。
     */
    public String toConditionRule() {
        if (type == NodeType.LEAF) {
            return toLeafConditionRule();
        }
        List<String> sqlParts = new ArrayList<>();
        for (ConditionRule child : children) {
            sqlParts.add(child.toConditionRule());
        }
        return "(" + String.join(" " + (logicalOp.equals(LOGICAL_OP.AND) ? "&&" : "||") + " ", sqlParts) + ")";
    }

    /**
     * 将当前规则转换为 SQL 条件语句字符串。
     */
    public String toSql() {
        if (type == NodeType.LEAF) {
            String sqlOperator = mapSqlOperator(operator);
            String sqlValue = mapSqlValue(sqlOperator, value);
            return String.format("%s %s %s", field, sqlOperator, sqlValue);
        }
        List<String> sqlParts = new ArrayList<>();
        for (ConditionRule child : children) {
            sqlParts.add(child.toSql());
        }
        return "(" + String.join(" " + logicalOp.name() + " ", sqlParts) + ")";
    }

    private String toLeafConditionRule() {
        String safeField = field == null ? "" : field.trim();
        String safeOperator = normalizeOperator(operator);
        if (StringUtils.isBlank(safeField) || StringUtils.isBlank(safeOperator)) {
            return "";
        }
        if ("CONTAINS".equals(safeOperator)) {
            List<String> keywords = toStringList(value);
            if (!keywords.isEmpty()) {
                return String.format("containsAnyText(%s, %s)", safeField, renderKeywordLiteral(keywords));
            }
            return String.format("containsIgnoreCase(%s, %s)", safeField, renderScriptLiteral(value, safeOperator));
        }
        if ("BETWEEN".equals(safeOperator)) {
            Object start = resolveBetweenValue(true);
            Object end = resolveBetweenValue(false);
            return String.format("(%s >= %s && %s <= %s)", safeField, renderScriptLiteral(start, "GTE"), safeField, renderScriptLiteral(end, "LTE"));
        }
        if ("IN".equals(safeOperator)) {
            List<String> keywords = toStringList(value);
            if (!keywords.isEmpty()) {
                return String.format("containsAnyText(%s, %s)", safeField, renderKeywordLiteral(keywords));
            }
            return String.format("containsIgnoreCase(%s, %s)", safeField, renderScriptLiteral(value, safeOperator));
        }
        String ruleOperator = mapRuleOperator(safeOperator);
        return String.format("%s %s %s", safeField, ruleOperator, renderScriptLiteral(value, safeOperator));
    }

    /**
     * 将规则操作符映射为条件规则操作符。
     */
    private String mapRuleOperator(String op) {
        if (op == null) {
            return "";
        }
        return switch (op.toUpperCase(Locale.ROOT)) {
            case "EQ" -> "==";
            case "GT" -> ">";
            case "LT" -> "<";
            case "GTE" -> ">=";
            case "LTE" -> "<=";
            case "BETWEEN" -> "BETWEEN";
            case "CONTAINS" -> "CONTAINS";
            case "IN" -> "IN";
            default -> op;
        };
    }

    /**
     * 将规则值映射为 SQL 值。
     */
    private String mapSqlValue(String operator, Object rawValue) {
        if (StringUtils.equals(operator, "IN")) {
            List<String> values = toStringList(rawValue);
            if (values.isEmpty()) {
                return "(" + renderSqlLiteral(rawValue) + ")";
            }
            List<String> renderedValues = new ArrayList<>(values.size());
            for (String item : values) {
                renderedValues.add("'" + item.replace("'", "''") + "'");
            }
            return "(" + String.join(",", renderedValues) + ")";
        }
        if (StringUtils.equals(operator, "BETWEEN")) {
            Object start = resolveBetweenValue(true);
            Object end = resolveBetweenValue(false);
            return renderSqlLiteral(start) + " AND " + renderSqlLiteral(end);
        }
        String val = rawValue == null ? null : String.valueOf(rawValue);
        if (StringUtils.containsAny(val, "${", "#{")) {
            return val;
        }
        if (rawValue instanceof Number || rawValue instanceof Boolean) {
            return String.valueOf(rawValue);
        }
        return "'" + String.valueOf(rawValue).replace("'", "''") + "'";
    }

    /**
     * 将规则操作符映射为 SQL 操作符。
     */
    private String mapSqlOperator(String op) {
        if (op == null) {
            return "";
        }
        return switch (op.toUpperCase(Locale.ROOT)) {
            case "EQ" -> "=";
            case "GT" -> ">";
            case "LT" -> "<";
            case "GTE" -> ">=";
            case "LTE" -> "<=";
            case "BETWEEN" -> "BETWEEN";
            case "IN" -> "IN";
            case "CONTAINS" -> "LIKE";
            default -> op;
        };
    }

    private String normalizeOperator(String op) {
        return op == null ? "" : op.trim().toUpperCase(Locale.ROOT);
    }

    private Object resolveBetweenValue(boolean first) {
        if (first && betweenValue1 != null) {
            return betweenValue1;
        }
        if (!first && betweenValue2 != null) {
            return betweenValue2;
        }
        if (value instanceof String text && text.contains("~")) {
            String[] parts = text.split("~", 2);
            if (parts.length == 2) {
                return first ? parts[0] : parts[1];
            }
        }
        if (value instanceof Collection<?> collection) {
            List<?> items = new ArrayList<>(collection);
            if (items.size() >= 2) {
                return first ? items.get(0) : items.get(1);
            }
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            if (length >= 2) {
                return first ? Array.get(value, 0) : Array.get(value, 1);
            }
        }
        return value;
    }

    private List<String> toStringList(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (rawValue instanceof Collection<?> collection) {
            for (Object item : collection) {
                String text = normalizeText(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            return values;
        }
        if (rawValue.getClass().isArray()) {
            int length = Array.getLength(rawValue);
            for (int index = 0; index < length; index++) {
                String text = normalizeText(Array.get(rawValue, index));
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            return values;
        }
        String text = normalizeText(rawValue);
        if (text.contains(",") || text.contains(";") || text.contains("|")) {
            for (String part : text.split("[,;|]")) {
                String normalized = part == null ? "" : part.trim();
                if (!normalized.isBlank()) {
                    values.add(normalized);
                }
            }
            return values;
        }
        if (!text.isBlank()) {
            values.add(text);
        }
        return values;
    }

    private String renderKeywordLiteral(List<String> keywords) {
        return "'" + String.join(",", keywords) + "'";
    }

    private String renderScriptLiteral(Object rawValue, String operator) {
        if (rawValue == null) {
            return "null";
        }
        if (rawValue instanceof Number || rawValue instanceof Boolean) {
            return String.valueOf(rawValue);
        }
        if (isNumericOperator(operator) && isNumericText(String.valueOf(rawValue))) {
            return String.valueOf(rawValue).trim();
        }
        if (rawValue instanceof Collection<?> || rawValue.getClass().isArray()) {
            return "'" + String.join(",", toStringList(rawValue)) + "'";
        }
        String text = String.valueOf(rawValue);
        if (StringUtils.containsAny(text, "${", "#{")) {
            return text;
        }
        return "'" + text.replace("'", "\\'") + "'";
    }

    private boolean isNumericOperator(String operator) {
        if (operator == null) {
            return false;
        }
        return switch (operator.toUpperCase(Locale.ROOT)) {
            case "GT", "GTE", "LT", "LTE", "BETWEEN", ">", ">=", "<", "<=" -> true;
            default -> false;
        };
    }

    private boolean isNumericText(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            new java.math.BigDecimal(trimmed);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String renderSqlLiteral(Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        if (rawValue instanceof Number || rawValue instanceof Boolean) {
            return String.valueOf(rawValue);
        }
        return "'" + String.valueOf(rawValue).replace("'", "''") + "'";
    }

    private String normalizeText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
