package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.util.*

data class WorkflowContext(
    @JsonProperty("version")
    var version: Int = 0,
    
    @JsonProperty("traceContext")
    var traceContext: MutableMap<String, Any> = TreeMap(),
    
    @JsonProperty("data")
    var data: MutableMap<String, Any> = TreeMap(),
    
    @JsonProperty("nodeStates")
    var nodeStates: MutableMap<String, NodeState> = TreeMap()
) : Serializable {
    
    fun incrementVersion(): WorkflowContext {
        version++
        return this
    }
    
    data class NodeState(
        @JsonProperty("nodeId")
        var nodeId: String? = null,
        
        @JsonProperty("status")
        var status: String? = null,
        
        @JsonProperty("outputs")
        var outputs: Map<String, Any>? = null,
        
        @JsonProperty("completionTime")
        var completionTime: Long = 0
    ) : Serializable
}
