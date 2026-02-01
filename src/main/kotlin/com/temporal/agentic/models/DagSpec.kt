package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class DagSpec(
    @JsonProperty("workflowId")
    var workflowId: String? = null,
    
    @JsonProperty("version")
    var version: Int = 0,
    
    @JsonProperty("maxParallelNodes")
    var maxParallelNodes: Int = 0,
    
    @JsonProperty("mergeStrategy")
    var mergeStrategy: String? = null,
    
    @JsonProperty("failureStrategy")
    var failureStrategy: String? = null,
    
    @JsonProperty("nodes")
    var nodes: List<NodeDefinition> = emptyList(),
    
    @JsonProperty("edges")
    var edges: List<EdgeDefinition> = emptyList(),
    
    @JsonProperty("executionSettings")
    var executionSettings: Map<String, Any>? = null
) : Serializable
