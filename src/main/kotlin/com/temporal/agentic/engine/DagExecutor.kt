package com.temporal.agentic.engine

import com.temporal.agentic.models.DagSpec
import com.temporal.agentic.models.NodeDefinition
import com.temporal.agentic.models.WorkflowContext
import java.util.*

class DagExecutor {
    
    private val topologyComputer = DagTopologyComputer()
    private val conditionEvaluator = ConditionEvaluator()
    private val mergeEngine = ContextMergeEngine()
    
    fun planExecution(dagSpec: DagSpec, context: WorkflowContext): DagExecutionPlan {
        val plan = DagExecutionPlan(
            dagVersion = dagSpec.version,
            maxParallelNodes = dagSpec.maxParallelNodes,
            totalNodes = dagSpec.nodes.size
        )
        
        val nodeStatuses = TreeMap<String, String>()
        
        for (node in dagSpec.nodes) {
            nodeStatuses[node.id!!] = "PENDING"
        }
        
        var stage = 0
        while (hasNonTerminalNodes(nodeStatuses)) {
            val readyNodes = topologyComputer.getReadyNodes(dagSpec, nodeStatuses, context, conditionEvaluator)
            
            if (readyNodes.isEmpty()) {
                break
            }
            
            val stagedNodes = readyNodes.take(dagSpec.maxParallelNodes)
            
            val stageInfo = DagExecutionPlan.Stage(
                stageNumber = stage,
                nodeIds = stagedNodes.toMutableList()
            )
            
            for (nodeId in stagedNodes) {
                nodeStatuses[nodeId] = "SCHEDULED"
            }
            
            plan.stages.add(stageInfo)
            stage++
        }
        
        return plan
    }
    
    fun buildNodeMap(dagSpec: DagSpec): Map<String, NodeDefinition> {
        val map = TreeMap<String, NodeDefinition>()
        for (node in dagSpec.nodes) {
            map[node.id!!] = node
        }
        return map
    }
    
    fun shouldReplan(completedNode: NodeDefinition): Boolean {
        return completedNode.policy?.replanCheckpoint == true
    }
    
    fun getMergeEngine(): ContextMergeEngine = mergeEngine
    
    fun getTopologyComputer(): DagTopologyComputer = topologyComputer
    
    fun getConditionEvaluator(): ConditionEvaluator = conditionEvaluator
    
    private fun hasNonTerminalNodes(statuses: Map<String, String>): Boolean {
        return statuses.values.any { it == "PENDING" || it == "SCHEDULED" }
    }
    
    data class DagExecutionPlan(
        var dagVersion: Int = 0,
        var maxParallelNodes: Int = 0,
        var totalNodes: Int = 0,
        val stages: MutableList<Stage> = mutableListOf()
    ) {
        data class Stage(
            var stageNumber: Int = 0,
            var nodeIds: MutableList<String> = mutableListOf()
        )
    }
}
