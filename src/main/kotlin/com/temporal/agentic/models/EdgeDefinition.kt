package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class EdgeDefinition(
    @JsonProperty("from")
    var from: String? = null,
    
    @JsonProperty("to")
    var to: String? = null,
    
    @JsonProperty("on")
    var on: String? = null,
    
    @JsonProperty("condition")
    var condition: String? = null
) : Serializable
