package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class FinalResponse(
    @JsonProperty("requestId")
    var requestId: String? = null,
    
    @JsonProperty("status")
    var status: String? = null,
    
    @JsonProperty("workflowStatus")
    var workflowStatus: WorkflowStatus? = null,
    
    @JsonProperty("outputs")
    var outputs: Map<String, Any>? = null,
    
    @JsonProperty("contextSummary")
    var contextSummary: MutableMap<String, Any>? = null,
    
    @JsonProperty("executionMetrics")
    var executionMetrics: Map<String, Any>? = null,
    
    @JsonProperty("errors")
    var errors: List<String>? = null
) : Serializable
