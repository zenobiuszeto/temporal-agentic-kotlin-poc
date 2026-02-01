package com.temporal.agentic.workflows

import com.temporal.agentic.engine.ContextMergeEngine
import com.temporal.agentic.engine.DagExecutor
import com.temporal.agentic.models.*
import io.temporal.workflow.*
import org.slf4j.Logger
import java.time.Duration
import java.util.*

class AgenticDagParentWorkflowImpl : AgenticDagParentWorkflow {
    
    private val logger: Logger = Workflow.getLogger(AgenticDagParentWorkflowImpl::class.java)
    
    private var workflowStatus = "STARTED"
    private lateinit var context: WorkflowContext
    private val nodeStatuses = TreeMap<String, String>()
    private lateinit var currentDagSpec: DagSpec
    private lateinit var dagExecutor: DagExecutor
    private lateinit var mergeEngine: ContextMergeEngine
    private var currentDagVersion = 1
    
    override fun executeWorkflow(request: InputRequest, initialDagSpec: DagSpec): FinalResponse {
        this.currentDagSpec = initialDagSpec
        this.dagExecutor = DagExecutor()
        this.mergeEngine = dagExecutor.getMergeEngine()
        
        context = WorkflowContext()
        context.traceContext["workflowRunId"] = Workflow.getInfo().workflowId
        context.traceContext["requestId"] = request.requestId!!
        
        val initialData = TreeMap<String, Any>()
        request.data?.let { initialData.putAll(it) }
        initialData["eventType"] = request.eventType!!
        initialData["channel"] = request.channel!!
        initialData["locale"] = request.locale!!
        context.data = initialData
        
        for (node in currentDagSpec.nodes) {
            nodeStatuses[node.id!!] = "PENDING"
        }
        
        logger.info("Starting agentic workflow for request: {}", request.requestId)
        
        return try {
            executeAgenticDAG(request)
            workflowStatus = "COMPLETED"
            buildFinalResponse(request, "SUCCESS")
        } catch (e: Exception) {
            logger.error("Workflow execution failed", e)
            workflowStatus = "FAILED"
            buildFinalResponse(request, "FAILED")
        }
    }
    
    private fun executeAgenticDAG(request: InputRequest) {
        val plannerNode = currentDagSpec.nodes.firstOrNull { it.type == "PLANNER" }
        
        if (plannerNode != null) {
            val plannerResponse = executePlannerNode(plannerNode, request)
            mergeContextFromResponse(plannerResponse)
            nodeStatuses["planner"] = "SUCCESS"
            
            plannerResponse.updatedDagSpec?.let { updatedSpec ->
                currentDagSpec = updatedSpec
                currentDagVersion = updatedSpec.version
                logger.info("DAG updated by planner to version {}", currentDagVersion)
                
                for (node in updatedSpec.nodes) {
                    nodeStatuses.putIfAbsent(node.id!!, "PENDING")
                }
            }
        }
        
        val nodeMap = dagExecutor.buildNodeMap(currentDagSpec)
        val executedNodes = mutableSetOf<String>()
        
        while (true) {
            // Get ready nodes based on what's been executed
            val readyNodes = dagExecutor.getTopologyComputer()
                .getReadyNodes(currentDagSpec, nodeStatuses, context, dagExecutor.getConditionEvaluator())
            
            if (readyNodes.isEmpty()) {
                break
            }
            
            // Take up to maxParallelNodes nodes for this batch
            val nodesToExecute = readyNodes
                .filter { !executedNodes.contains(it) }
                .take(currentDagSpec.maxParallelNodes)
            
            if (nodesToExecute.isEmpty()) {
                break
            }
            
            val futures = mutableListOf<Promise<NodeExecutionResponse?>>()
            
            for (nodeId in nodesToExecute) {
                nodeStatuses[nodeId] = "RUNNING"
                futures.add(executeNodeAsync(nodeId, nodeMap[nodeId]!!))
            }
            
            Promise.allOf(futures).get()
            
            for (future in futures) {
                try {
                    val response = future.get()
                    if (response != null) {
                        executedNodes.add(response.nodeId!!)
                        mergeContextFromResponse(response)
                        
                        val nodeDef = nodeMap[response.nodeId]
                        if (nodeDef?.policy?.replanCheckpoint == true) {
                            logger.info("Replan checkpoint reached at node {}", response.nodeId)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get node response", e)
                }
            }
        }
        
        logger.info("Agentic DAG execution completed.")
    }
    
    private fun executePlannerNode(plannerNode: NodeDefinition, request: InputRequest): NodeExecutionResponse {
        val child = Workflow.newChildWorkflowStub(
            NodeChildWorkflow::class.java,
            ChildWorkflowOptions.newBuilder()
                .setTaskQueue(plannerNode.taskQueue ?: "default")
                .setWorkflowExecutionTimeout(Duration.ofSeconds(600))
                .build()
        )
        
        return child.executeNode(
            NodeExecutionRequest(
                workflowRunId = Workflow.getInfo().workflowId,
                nodeId = "planner",
                nodeType = "PLANNER",
                nodeInput = mapOf(
                    "eventType" to request.eventType!!,
                    "channel" to request.channel!!,
                    "locale" to request.locale!!
                ),
                contextSnapshot = mergeEngine.captureContextSnapshot(context),
                idempotencyKey = "planner-${Workflow.getInfo().workflowId}",
                attempt = 1,
                traceContext = context.traceContext
            )
        )
    }
    
    private fun executeNodeAsync(nodeId: String, nodeDef: NodeDefinition): Promise<NodeExecutionResponse?> {
        return Async.function {
            try {
                if (nodeDef.type == "PLANNER") {
                    null
                } else {
                
                val child = Workflow.newChildWorkflowStub(
                    NodeChildWorkflow::class.java,
                    ChildWorkflowOptions.newBuilder()
                        .setTaskQueue(nodeDef.taskQueue ?: "default")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(600))
                        .build()
                )
                
                val snapshot = mergeEngine.captureContextSnapshot(context)
                val nodeInputs = resolveNodeInputs(nodeDef, context)
                
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
                }
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
    
    private fun mergeContextFromResponse(response: NodeExecutionResponse?) {
        if (response != null && (response.status == "SUCCESS" || response.status == "SKIPPED")) {
            mergeEngine.mergeContextDelta(context, response, System.currentTimeMillis())
        }
    }
    
    private fun resolveNodeInputs(nodeDef: NodeDefinition, context: WorkflowContext): Map<String, Any> {
        val resolved = TreeMap<String, Any>()
        nodeDef.inputs ?: return resolved
        
        for ((key, value) in nodeDef.inputs!!) {
            when {
                value is String && value.startsWith("ref(") -> {
                    val refPath = value.substring(4, value.length - 1)
                    resolveContextPath(refPath, context)?.let { resolved[key] = it }
                }
                value == "required" -> {
                    context.data[key]?.let { resolved[key] = it }
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
            totalNodes = currentDagSpec.nodes.size,
            completedNodes = nodeStatuses.values.count { it == "SUCCESS" || it == "SKIPPED" },
            failedNodes = nodeStatuses.values.count { it == "FAILED" }
        )
        
        val contextSummary = TreeMap<String, Any>()
        contextSummary["version"] = context.version
        contextSummary["dagVersion"] = currentDagVersion
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
            put("dagVersion", currentDagVersion)
            put("nodeStates", context.nodeStates.keys)
            put("status", workflowStatus)
        }
    }
    
    override fun getCurrentDagVersion(): Int = currentDagVersion
}
