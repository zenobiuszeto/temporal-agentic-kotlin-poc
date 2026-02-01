package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class WorkflowStatus(
    @JsonProperty("status")
    var status: String? = null,
    
    @JsonProperty("completedNodes")
    var completedNodes: Int = 0,
    
    @JsonProperty("totalNodes")
    var totalNodes: Int = 0,
    
    @JsonProperty("failedNodes")
    var failedNodes: Int = 0
) : Serializable
