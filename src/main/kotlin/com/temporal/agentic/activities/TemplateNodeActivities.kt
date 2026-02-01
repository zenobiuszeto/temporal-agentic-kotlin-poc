package com.temporal.agentic.activities

import com.temporal.agentic.models.NodeExecutionRequest
import com.temporal.agentic.models.NodeExecutionResponse

interface TemplateNodeActivities {
    
    fun executeValidate(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeGovernance(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeRetrieve(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeTemplateMap(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeTransform(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeDecide(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeAction(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executeFinalize(request: NodeExecutionRequest): NodeExecutionResponse
    
    fun executePlanner(request: NodeExecutionRequest): NodeExecutionResponse
}
