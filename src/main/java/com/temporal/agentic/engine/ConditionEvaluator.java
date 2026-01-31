package com.temporal.agentic.engine;

import com.temporal.agentic.models.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Expression evaluator for conditions in DAG edges and nodes.
 * Supports simple deterministic expressions: exists(path), equals(path, value), and/or/not
 */
public class ConditionEvaluator {

    public boolean evaluate(String condition, Map<String, Object> context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }
        return evaluateExpression(condition.trim(), context);
    }

    private boolean evaluateExpression(String expr, Map<String, Object> context) {
        expr = expr.trim();

        if (expr.startsWith("!")) {
            return !evaluateExpression(expr.substring(1).trim(), context);
        }

        if (expr.contains(" and ")) {
            String[] parts = expr.split(" and ");
            for (String part : parts) {
                if (!evaluateExpression(part.trim(), context)) {
                    return false;
                }
            }
            return true;
        }

        if (expr.contains(" or ")) {
            String[] parts = expr.split(" or ");
            for (String part : parts) {
                if (evaluateExpression(part.trim(), context)) {
                    return true;
                }
            }
            return false;
        }

        if (expr.startsWith("exists(") && expr.endsWith(")")) {
            String path = expr.substring(7, expr.length() - 1);
            return exists(path, context);
        }

        if (expr.startsWith("equals(") && expr.endsWith(")")) {
            String inner = expr.substring(7, expr.length() - 1);
            String[] parts = splitEqualsArgs(inner);
            if (parts.length == 2) {
                Object val1 = resolveValue(parts[0].trim(), context);
                Object val2 = resolveValue(parts[1].trim(), context);
                return Objects.equals(val1, val2);
            }
        }

        return false;
    }

    private boolean exists(String path, Map<String, Object> context) {
        return getValueAtPath(path, context) != null;
    }

    private Object resolveValue(String valueStr, Map<String, Object> context) {
        valueStr = valueStr.trim();
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }
        try {
            return Long.parseLong(valueStr);
        } catch (NumberFormatException e) {
            // Not a number
        }
        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }
        return getValueAtPath(valueStr, context);
    }

    private Object getValueAtPath(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object current = context;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private String[] splitEqualsArgs(String inner) {
        int depth = 0;
        for (int i = 0; i < inner.length(); i++) {
            if (inner.charAt(i) == '(') depth++;
            if (inner.charAt(i) == ')') depth--;
            if (depth == 0 && inner.charAt(i) == ',') {
                return new String[]{inner.substring(0, i), inner.substring(i + 1)};
            }
        }
        return inner.split(",");
    }
}
