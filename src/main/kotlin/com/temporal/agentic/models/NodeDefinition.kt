package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class NodeDefinition(
    @JsonProperty("id")
    var id: String? = null,
    
    @JsonProperty("type")
    var type: String? = null,
    
    @JsonProperty("name")
    var name: String? = null,
    
    @JsonProperty("inputs")
    var inputs: Map<String, Any>? = null,
    
    @JsonProperty("outputs")
    var outputs: Map<String, Any>? = null,
    
    @JsonProperty("taskQueue")
    var taskQueue: String? = null,
    
    @JsonProperty("policy")
    var policy: NodePolicy? = null,
    
    @JsonProperty("timeouts")
    var timeouts: TimeoutSpec? = null,
    
    @JsonProperty("retryPolicy")
    var retryPolicy: RetrySpec? = null,
    
    @JsonProperty("compensationNodeId")
    var compensationNodeId: String? = null,
    
    @JsonProperty("condition")
    var condition: String? = null
) : Serializable
