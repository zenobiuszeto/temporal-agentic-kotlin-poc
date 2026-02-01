package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class NodeExecutionRequest(
    @JsonProperty("workflowRunId")
    var workflowRunId: String? = null,
    
    @JsonProperty("nodeId")
    var nodeId: String? = null,
    
    @JsonProperty("nodeType")
    var nodeType: String? = null,
    
    @JsonProperty("nodeInput")
    var nodeInput: Map<String, Any>? = null,
    
    @JsonProperty("contextSnapshot")
    var contextSnapshot: ContextSnapshot? = null,
    
    @JsonProperty("idempotencyKey")
    var idempotencyKey: String? = null,
    
    @JsonProperty("attempt")
    var attempt: Int = 0,
    
    @JsonProperty("traceContext")
    var traceContext: Map<String, Any>? = null
) : Serializable
