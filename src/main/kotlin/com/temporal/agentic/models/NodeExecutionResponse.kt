package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class NodeExecutionResponse(
    @JsonProperty("nodeId")
    var nodeId: String? = null,
    
    @JsonProperty("status")
    var status: String? = null,
    
    @JsonProperty("outputs")
    var outputs: Map<String, Any>? = null,
    
    @JsonProperty("contextDelta")
    var contextDelta: ContextDelta? = null,
    
    @JsonProperty("errors")
    var errors: List<String>? = null,
    
    @JsonProperty("metrics")
    var metrics: Map<String, Any>? = null,
    
    @JsonProperty("updatedDagSpec")
    var updatedDagSpec: DagSpec? = null
) : Serializable
