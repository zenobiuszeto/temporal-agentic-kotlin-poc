package com.temporal.agentic.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class TimeoutSpec(
    @JsonProperty("startToCloseSeconds")
    var startToCloseSeconds: Long = 600,
    
    @JsonProperty("scheduleToCloseSeconds")
    var scheduleToCloseSeconds: Long = 3600
) : Serializable
