package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class RetrySpec(
    @JsonProperty("initialIntervalSeconds")
    var initialIntervalSeconds: Long = 1,
    
    @JsonProperty("backoffCoefficient")
    var backoffCoefficient: Double = 2.0,
    
    @JsonProperty("maximumAttempts")
    var maximumAttempts: Int = 3
) : Serializable
