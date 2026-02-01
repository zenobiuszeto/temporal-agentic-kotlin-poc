package com.temporal.agentic.workflows

import com.temporal.agentic.models.NodeExecutionRequest
import com.temporal.agentic.models.NodeExecutionResponse
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface NodeChildWorkflow {
    
    @WorkflowMethod
    fun executeNode(request: NodeExecutionRequest): NodeExecutionResponse
}
