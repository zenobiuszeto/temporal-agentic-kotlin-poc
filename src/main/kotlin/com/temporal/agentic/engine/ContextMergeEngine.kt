package com.temporal.agentic.engine

import com.temporal.agentic.models.*
import java.util.*

class ContextMergeEngine {
    
    fun mergeContextDelta(
        context: WorkflowContext,
        response: NodeExecutionResponse,
        completionTime: Long
    ) {
        val delta = response.contextDelta ?: return
        
        val nodeId = response.nodeId!!
        val contextData = context.data
        
        val nodeNamespace = contextData.computeIfAbsent("nodes") { TreeMap<String, Any>() } as MutableMap<String, Any>
        val nodeData = nodeNamespace.computeIfAbsent(nodeId) { TreeMap<String, Any>() } as MutableMap<String, Any>
        
        delta.writes?.forEach { (key, value) ->
            nodeData[key] = value
        }
        
        delta.promoteKeys?.forEach { key ->
            nodeData[key]?.let { value ->
                contextData[key] = value
            }
        }
        
        val nodeState = WorkflowContext.NodeState(
            nodeId = nodeId,
            status = response.status,
            outputs = response.outputs,
            completionTime = completionTime
        )
        context.nodeStates[nodeId] = nodeState
        
        context.incrementVersion()
    }
    
    fun applyMultipleDeltas(
        context: WorkflowContext,
        responses: List<NodeExecutionResponse>
    ) {
        val sorted = responses.sortedBy { it.nodeId }
        
        var timestamp = System.currentTimeMillis()
        for (response in sorted) {
            mergeContextDelta(context, response, timestamp++)
        }
    }
    
    fun captureContextSnapshot(context: WorkflowContext): ContextSnapshot {
        return ContextSnapshot(
            contextVersion = context.version,
            traceContext = TreeMap(context.traceContext),
            data = deepCopyMap(context.data)
        )
    }
    
    private fun deepCopyMap(original: Map<String, Any>): Map<String, Any> {
        val copy = TreeMap<String, Any>()
        for ((key, value) in original) {
            copy[key] = when (value) {
                is Map<*, *> -> deepCopyMap(value as Map<String, Any>)
                is List<*> -> ArrayList(value)
                else -> value
            }
        }
        return copy
    }
}
