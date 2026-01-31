package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Node execution policy.
 */
public class NodePolicy implements Serializable {
    @JsonProperty("allowFailure")
    private boolean allowFailure;

    @JsonProperty("allowRerun")
    private boolean allowRerun;

    @JsonProperty("isTerminal")
    private boolean isTerminal;

    @JsonProperty("replanCheckpoint")
    private boolean replanCheckpoint;

    public NodePolicy() {
        this.allowFailure = false;
        this.allowRerun = false;
        this.isTerminal = false;
        this.replanCheckpoint = false;
    }

    public NodePolicy(boolean allowFailure, boolean allowRerun, boolean isTerminal, boolean replanCheckpoint) {
        this.allowFailure = allowFailure;
        this.allowRerun = allowRerun;
        this.isTerminal = isTerminal;
        this.replanCheckpoint = replanCheckpoint;
    }

    public boolean isAllowFailure() { return allowFailure; }
    public void setAllowFailure(boolean allowFailure) { this.allowFailure = allowFailure; }

    public boolean isAllowRerun() { return allowRerun; }
    public void setAllowRerun(boolean allowRerun) { this.allowRerun = allowRerun; }

    public boolean isTerminal() { return isTerminal; }
    public void setTerminal(boolean terminal) { isTerminal = terminal; }

    public boolean isReplanCheckpoint() { return replanCheckpoint; }
    public void setReplanCheckpoint(boolean replanCheckpoint) { this.replanCheckpoint = replanCheckpoint; }
}
