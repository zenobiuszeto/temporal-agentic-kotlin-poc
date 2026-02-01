package com.temporal.agentic.workflows

import com.temporal.agentic.activities.TemplateNodeActivities
import com.temporal.agentic.models.NodeExecutionRequest
import com.temporal.agentic.models.NodeExecutionResponse
import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import org.slf4j.Logger
import java.time.Duration
import java.util.*

class NodeChildWorkflowImpl : NodeChildWorkflow {
    
    private val logger: Logger = Workflow.getLogger(NodeChildWorkflowImpl::class.java)
    
    private val activities: TemplateNodeActivities = Workflow.newActivityStub(
        TemplateNodeActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(120))
            .build()
    )
    
    override fun executeNode(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeType = request.nodeType!!
        val nodeId = request.nodeId!!
        
        logger.info("Executing node {} of type {}", nodeId, nodeType)
        
        return try {
            routeNodeExecution(request, nodeType)
        } catch (e: Exception) {
            logger.error("Node execution failed: $nodeId", e)
            createFailedResponse(request, e.message ?: "Unknown error")
        }
    }
    
    private fun routeNodeExecution(request: NodeExecutionRequest, nodeType: String): NodeExecutionResponse {
        return when (nodeType.uppercase()) {
            "VALIDATE" -> activities.executeValidate(request)
            "GOVERNANCE" -> activities.executeGovernance(request)
            "RETRIEVE" -> activities.executeRetrieve(request)
            "TEMPLATE_MAP" -> activities.executeTemplateMap(request)
            "TRANSFORM" -> activities.executeTransform(request)
            "DECIDE" -> activities.executeDecide(request)
            "ACTION" -> activities.executeAction(request)
            "FINALIZE" -> activities.executeFinalize(request)
            "PLANNER" -> activities.executePlanner(request)
            else -> throw IllegalArgumentException("Unknown node type: $nodeType")
        }
    }
    
    private fun createFailedResponse(request: NodeExecutionRequest, errorMessage: String): NodeExecutionResponse {
        return NodeExecutionResponse(
            nodeId = request.nodeId,
            status = "FAILED",
            outputs = TreeMap(),
            errors = listOf(errorMessage),
            metrics = TreeMap()
        )
    }
}
