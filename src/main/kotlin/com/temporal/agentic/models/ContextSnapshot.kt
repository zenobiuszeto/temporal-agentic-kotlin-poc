package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class ContextSnapshot(
    @JsonProperty("contextVersion")
    var contextVersion: Int = 0,
    
    @JsonProperty("traceContext")
    var traceContext: Map<String, Any>? = null,
    
    @JsonProperty("data")
    var data: Map<String, Any>? = null
) : Serializable
