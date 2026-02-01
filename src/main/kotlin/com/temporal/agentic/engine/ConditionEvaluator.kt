package com.temporal.agentic.engine

import java.util.*

class ConditionEvaluator {
    
    fun evaluate(condition: String?, context: Map<String, Any>): Boolean {
        if (condition.isNullOrBlank()) {
            return true
        }
        return evaluateExpression(condition.trim(), context)
    }
    
    private fun evaluateExpression(expr: String, context: Map<String, Any>): Boolean {
        var expression = expr.trim()
        
        if (expression.startsWith("!")) {
            return !evaluateExpression(expression.substring(1).trim(), context)
        }
        
        if (expression.contains(" and ")) {
            val parts = expression.split(" and ")
            return parts.all { evaluateExpression(it.trim(), context) }
        }
        
        if (expression.contains(" or ")) {
            val parts = expression.split(" or ")
            return parts.any { evaluateExpression(it.trim(), context) }
        }
        
        if (expression.startsWith("exists(") && expression.endsWith(")")) {
            val path = expression.substring(7, expression.length - 1)
            return exists(path, context)
        }
        
        if (expression.startsWith("equals(") && expression.endsWith(")")) {
            val inner = expression.substring(7, expression.length - 1)
            val parts = splitEqualsArgs(inner)
            if (parts.size == 2) {
                val val1 = resolveValue(parts[0].trim(), context)
                val val2 = resolveValue(parts[1].trim(), context)
                return val1 == val2
            }
        }
        
        return false
    }
    
    private fun exists(path: String, context: Map<String, Any>): Boolean {
        return getValueAtPath(path, context) != null
    }
    
    private fun resolveValue(valueStr: String, context: Map<String, Any>): Any? {
        val trimmed = valueStr.trim()
        
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        
        trimmed.toLongOrNull()?.let { return it }
        
        when (trimmed.lowercase()) {
            "true" -> return true
            "false" -> return false
        }
        
        return getValueAtPath(trimmed, context)
    }
    
    private fun getValueAtPath(path: String, context: Map<String, Any>): Any? {
        val parts = path.split(".")
        var current: Any? = context
        
        for (part in parts) {
            current = when (current) {
                is Map<*, *> -> current[part]
                else -> return null
            }
        }
        
        return current
    }
    
    private fun splitEqualsArgs(inner: String): List<String> {
        var depth = 0
        for (i in inner.indices) {
            when (inner[i]) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) {
                    return listOf(inner.substring(0, i), inner.substring(i + 1))
                }
            }
        }
        return inner.split(",")
    }
}
