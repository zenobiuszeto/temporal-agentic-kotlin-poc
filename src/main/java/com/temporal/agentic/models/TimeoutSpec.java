package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Timeout configuration for nodes.
 */
public class TimeoutSpec implements Serializable {
    @JsonProperty("startToCloseSeconds")
    private long startToCloseSeconds;

    @JsonProperty("scheduleToCloseSeconds")
    private long scheduleToCloseSeconds;

    public TimeoutSpec() {
        this.startToCloseSeconds = 600; // 10 minutes
        this.scheduleToCloseSeconds = 3600; // 1 hour
    }

    public TimeoutSpec(long startToCloseSeconds, long scheduleToCloseSeconds) {
        this.startToCloseSeconds = startToCloseSeconds;
        this.scheduleToCloseSeconds = scheduleToCloseSeconds;
    }

    public long getStartToCloseSeconds() { return startToCloseSeconds; }
    public void setStartToCloseSeconds(long startToCloseSeconds) { this.startToCloseSeconds = startToCloseSeconds; }

    public long getScheduleToCloseSeconds() { return scheduleToCloseSeconds; }
    public void setScheduleToCloseSeconds(long scheduleToCloseSeconds) { this.scheduleToCloseSeconds = scheduleToCloseSeconds; }
}
