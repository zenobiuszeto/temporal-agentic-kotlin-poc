package com.temporal.agentic.workflows

import com.temporal.agentic.models.DagSpec
import com.temporal.agentic.models.FinalResponse
import com.temporal.agentic.models.InputRequest
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface NonAgenticDagParentWorkflow {
    
    @WorkflowMethod
    fun executeWorkflow(request: InputRequest, dagSpec: DagSpec): FinalResponse
    
    @QueryMethod
    fun getStatus(): String
    
    @QueryMethod
    fun getNodeStatuses(): Map<String, String>
    
    @QueryMethod
    fun getContextSummary(): Map<String, Any>
}
