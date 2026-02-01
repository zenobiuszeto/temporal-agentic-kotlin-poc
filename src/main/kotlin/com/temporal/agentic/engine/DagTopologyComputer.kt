package com.temporal.agentic.engine

import com.temporal.agentic.models.DagSpec
import com.temporal.agentic.models.EdgeDefinition
import com.temporal.agentic.models.NodeDefinition
import com.temporal.agentic.models.WorkflowContext
import java.util.*

class DagTopologyComputer {
    
    fun computeTopologicalLevels(dagSpec: DagSpec): Map<String, Int> {
        val levels = TreeMap<String, Int>()
        val incomingEdges = buildIncomingEdgesMap(dagSpec)
        
        for (node in dagSpec.nodes) {
            levels[node.id!!] = 0
        }
        
        var changed = true
        while (changed) {
            changed = false
            for (node in dagSpec.nodes) {
                val incoming = incomingEdges[node.id] ?: emptySet()
                var maxLevel = 0
                for (inNodeId in incoming) {
                    val inLevel = levels[inNodeId] ?: 0
                    if (inLevel >= maxLevel) {
                        maxLevel = inLevel + 1
                    }
                }
                val currentLevel = levels[node.id]!!
                if (maxLevel > currentLevel) {
                    levels[node.id!!] = maxLevel
                    changed = true
                }
            }
        }
        
        return levels
    }
    
    fun getReadyNodes(
        dagSpec: DagSpec,
        nodeStatuses: Map<String, String>,
        context: WorkflowContext,
        evaluator: ConditionEvaluator
    ): Set<String> {
        val ready = TreeSet<String>()
        val incomingEdges = buildIncomingEdgesMap(dagSpec)
        
        for (node in dagSpec.nodes) {
            if (nodeStatuses[node.id] == "PENDING") {
                val incoming = incomingEdges[node.id] ?: emptySet()
                
                val allPredecessorsComplete = incoming.all { predId ->
                    val predStatus = nodeStatuses[predId] ?: "PENDING"
                    predStatus == "SUCCESS" || predStatus == "SKIPPED"
                }
                
                if (allPredecessorsComplete) {
                    val condition = node.condition
                    if (condition.isNullOrBlank() || evaluator.evaluate(condition, context.data)) {
                        ready.add(node.id!!)
                    }
                }
            }
        }
        
        return ready
    }
    
    private fun buildIncomingEdgesMap(dagSpec: DagSpec): Map<String, Set<String>> {
        val map = TreeMap<String, MutableSet<String>>()
        for (edge in dagSpec.edges) {
            map.computeIfAbsent(edge.to!!) { TreeSet() }.add(edge.from!!)
        }
        return map
    }
    
    private fun buildOutgoingEdgesMap(dagSpec: DagSpec): Map<String, Set<String>> {
        val map = TreeMap<String, MutableSet<String>>()
        for (edge in dagSpec.edges) {
            map.computeIfAbsent(edge.from!!) { TreeSet() }.add(edge.to!!)
        }
        return map
    }
    
    fun computeDeterministicNodeOrder(readyNodes: Set<String>): List<String> {
        return readyNodes.sorted()
    }
}
