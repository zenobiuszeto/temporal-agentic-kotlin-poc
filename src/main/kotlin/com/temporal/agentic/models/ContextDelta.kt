package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class ContextDelta(
    @JsonProperty("nodeId")
    var nodeId: String? = null,
    
    @JsonProperty("writes")
    var writes: Map<String, Any>? = null,
    
    @JsonProperty("promoteKeys")
    var promoteKeys: List<String>? = null,
    
    @JsonProperty("timestamp")
    var timestamp: Long = 0
) : Serializable
