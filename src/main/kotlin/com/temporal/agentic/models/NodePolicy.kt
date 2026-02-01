package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class NodePolicy(
    @JsonProperty("allowFailure")
    var allowFailure: Boolean = false,
    
    @JsonProperty("allowRerun")
    var allowRerun: Boolean = false,
    
    @JsonProperty("isTerminal")
    var isTerminal: Boolean = false,
    
    @JsonProperty("replanCheckpoint")
    var replanCheckpoint: Boolean = false
) : Serializable
