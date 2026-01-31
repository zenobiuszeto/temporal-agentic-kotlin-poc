package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Edge definition in DAG spec.
 */
public class EdgeDefinition implements Serializable {
    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("on")
    private String on; // "success", "failure", "always"

    @JsonProperty("condition")
    private String condition;

    public EdgeDefinition() {}

    public EdgeDefinition(String from, String to, String on, String condition) {
        this.from = from;
        this.to = to;
        this.on = on;
        this.condition = condition;
    }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getOn() { return on; }
    public void setOn(String on) { this.on = on; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
