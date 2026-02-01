package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class InputRequest(
    @JsonProperty("requestId")
    var requestId: String? = null,
    
    @JsonProperty("workflowId")
    var workflowId: String? = null,
    
    @JsonProperty("eventType")
    var eventType: String? = null,
    
    @JsonProperty("channel")
    var channel: String? = null,
    
    @JsonProperty("locale")
    var locale: String? = null,
    
    @JsonProperty("data")
    var data: Map<String, Any>? = null,
    
    @JsonProperty("params")
    var params: Map<String, Any>? = null
) : Serializable
