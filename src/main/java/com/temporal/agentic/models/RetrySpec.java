package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Retry policy for nodes.
 */
public class RetrySpec implements Serializable {
    @JsonProperty("initialIntervalSeconds")
    private long initialIntervalSeconds;

    @JsonProperty("backoffCoefficient")
    private double backoffCoefficient;

    @JsonProperty("maximumAttempts")
    private int maximumAttempts;

    public RetrySpec() {
        this.initialIntervalSeconds = 1;
        this.backoffCoefficient = 2.0;
        this.maximumAttempts = 3;
    }

    public RetrySpec(long initialIntervalSeconds, double backoffCoefficient, int maximumAttempts) {
        this.initialIntervalSeconds = initialIntervalSeconds;
        this.backoffCoefficient = backoffCoefficient;
        this.maximumAttempts = maximumAttempts;
    }

    public long getInitialIntervalSeconds() { return initialIntervalSeconds; }
    public void setInitialIntervalSeconds(long initialIntervalSeconds) { this.initialIntervalSeconds = initialIntervalSeconds; }

    public double getBackoffCoefficient() { return backoffCoefficient; }
    public void setBackoffCoefficient(double backoffCoefficient) { this.backoffCoefficient = backoffCoefficient; }

    public int getMaximumAttempts() { return maximumAttempts; }
    public void setMaximumAttempts(int maximumAttempts) { this.maximumAttempts = maximumAttempts; }
}
