package com.temporal.agentic.workflows

import com.temporal.agentic.engine.ContextMergeEngine
import com.temporal.agentic.engine.DagExecutor
import com.temporal.agentic.models.*
import io.temporal.workflow.*
import org.slf4j.Logger
import java.time.Duration
import java.util.*

class NonAgenticDagParentWorkflowImpl : NonAgenticDagParentWorkflow {
    
    private val logger: Logger = Workflow.getLogger(NonAgenticDagParentWorkflowImpl::class.java)
    
    private var workflowStatus = "STARTED"
    private lateinit var context: WorkflowContext
    private val nodeStatuses = TreeMap<String, String>()
    private lateinit var dagSpec: DagSpec
    private lateinit var dagExecutor: DagExecutor
    private lateinit var mergeEngine: ContextMergeEngine
    
    override fun executeWorkflow(request: InputRequest, dagSpec: DagSpec): FinalResponse {
        this.dagSpec = dagSpec
        this.dagExecutor = DagExecutor()
        this.mergeEngine = dagExecutor.getMergeEngine()
        
        context = WorkflowContext()
        context.traceContext["workflowRunId"] = Workflow.getInfo().workflowId
        context.traceContext["requestId"] = request.requestId!!
        
        val initialData = TreeMap<String, Any>()
        request.data?.let { initialData.putAll(it) }
        context.data = initialData
        
        for (node in dagSpec.nodes) {
            nodeStatuses[node.id!!] = "PENDING"
        }
        
        logger.info("Starting non-agentic workflow for request: {}", request.requestId)
        
        return try {
            executeDAG()
            workflowStatus = "COMPLETED"
            buildFinalResponse(request, "SUCCESS")
        } catch (e: Exception) {
            logger.error("Workflow execution failed", e)
            workflowStatus = "FAILED"
            buildFinalResponse(request, "FAILED")
        }
    }
    
    private fun executeDAG() {
        val nodeMap = dagExecutor.buildNodeMap(dagSpec)
        val executedNodes = mutableListOf<String>()
        
        while (true) {
            val plan = dagExecutor.planExecution(dagSpec, context)
            if (plan.stages.isEmpty()) {
                break
            }
            
            for (stage in plan.stages) {
                val futures = mutableListOf<Promise<NodeExecutionResponse>>()
                
                for (nodeId in stage.nodeIds) {
                    nodeStatuses[nodeId] = "RUNNING"
                    futures.add(executeNodeAsync(nodeId, nodeMap[nodeId]!!))
                }
                
                Promise.allOf(futures).get()
                
                for (future in futures) {
                    try {
                        val response = future.get()
                        executedNodes.add(response.nodeId!!)
                        mergeContextFromResponse(response)
                    } catch (e: Exception) {
                        logger.error("Failed to get node response", e)
                    }
                }
            }
        }
        
        logger.info("DAG execution completed. Executed nodes: {}", executedNodes)
    }
    
    private fun executeNodeAsync(nodeId: String, nodeDef: NodeDefinition): Promise<NodeExecutionResponse> {
        return Async.function {
            try {
                val child = Workflow.newChildWorkflowStub(
                    NodeChildWorkflow::class.java,
                    ChildWorkflowOptions.newBuilder()
                        .setTaskQueue(nodeDef.taskQueue ?: "default")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(600))
                        .build()
                )
                
                val snapshot = mergeEngine.captureContextSnapshot(context)
                val nodeInputs = resolveNodeInputs(nodeDef, nodeMap(), context)
                
                val childRequest = NodeExecutionRequest(
                    workflowRunId = Workflow.getInfo().workflowId,
                    nodeId = nodeId,
                    nodeType = nodeDef.type,
                    nodeInput = nodeInputs,
                    contextSnapshot = snapshot,
                    idempotencyKey = "$nodeId-${Workflow.getInfo().workflowId}",
                    attempt = 1,
                    traceContext = context.traceContext
                )
                
                val response = child.executeNode(childRequest)
                nodeStatuses[nodeId] = response.status!!
                
                logger.info("Node {} completed with status: {}", nodeId, response.status)
                response
            } catch (e: Exception) {
                logger.error("Node {} execution error: {}", nodeId, e.message)
                NodeExecutionResponse(
                    nodeId = nodeId,
                    status = "FAILED",
                    errors = listOf(e.message ?: "Unknown error")
                )
            }
        }
    }
    
    private fun nodeMap(): Map<String, NodeDefinition> {
        return dagExecutor.buildNodeMap(dagSpec)
    }
    
    private fun mergeContextFromResponse(response: NodeExecutionResponse) {
        if (response.status == "SUCCESS" || response.status == "SKIPPED") {
            mergeEngine.mergeContextDelta(context, response, System.currentTimeMillis())
        }
    }
    
    private fun resolveNodeInputs(
        nodeDef: NodeDefinition,
        nodeMap: Map<String, NodeDefinition>,
        context: WorkflowContext
    ): Map<String, Any> {
        val resolved = TreeMap<String, Any>()
        nodeDef.inputs ?: return resolved
        
        for ((key, value) in nodeDef.inputs!!) {
            when {
                value is String && value.startsWith("ref(") -> {
                    val refPath = value.substring(4, value.length - 1)
                    resolveContextPath(refPath, context)?.let { resolved[key] = it }
                }
                value == "required" -> {
                    // Will be filled from input request
                }
                else -> {
                    resolved[key] = value
                }
            }
        }
        
        return resolved
    }
    
    private fun resolveContextPath(path: String, context: WorkflowContext): Any? {
        val parts = path.split(".")
        var current: Any? = context.data["nodes"] ?: TreeMap<String, Any>()
        
        for (part in parts) {
            current = when (current) {
                is Map<*, *> -> current[part]
                else -> return null
            }
        }
        
        return current
    }
    
    private fun buildFinalResponse(request: InputRequest, status: String): FinalResponse {
        val workflowStat = WorkflowStatus(
            status = workflowStatus,
            totalNodes = dagSpec.nodes.size,
            completedNodes = nodeStatuses.values.count { it == "SUCCESS" || it == "SKIPPED" },
            failedNodes = nodeStatuses.values.count { it == "FAILED" }
        )
        
        val contextSummary = TreeMap<String, Any>()
        contextSummary["version"] = context.version
        contextSummary["nodeCount"] = nodeStatuses.size
        
        return FinalResponse(
            requestId = request.requestId,
            status = status,
            workflowStatus = workflowStat,
            outputs = TreeMap(context.data),
            contextSummary = contextSummary,
            errors = emptyList()
        )
    }
    
    override fun getStatus(): String = workflowStatus
    
    override fun getNodeStatuses(): Map<String, String> = TreeMap(nodeStatuses)
    
    override fun getContextSummary(): Map<String, Any> {
        return TreeMap<String, Any>().apply {
            put("contextVersion", context.version)
            put("nodeStates", context.nodeStates.keys)
            put("status", workflowStatus)
        }
    }
}
