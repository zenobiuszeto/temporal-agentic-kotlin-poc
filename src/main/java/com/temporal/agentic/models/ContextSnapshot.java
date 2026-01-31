package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * Snapshot of context passed to child workflows.
 */
public class ContextSnapshot implements Serializable {
    @JsonProperty("contextVersion")
    private int contextVersion;

    @JsonProperty("traceContext")
    private Map<String, Object> traceContext;

    @JsonProperty("data")
    private Map<String, Object> data;

    public ContextSnapshot() {}

    public ContextSnapshot(int contextVersion, Map<String, Object> traceContext, Map<String, Object> data) {
        this.contextVersion = contextVersion;
        this.traceContext = traceContext;
        this.data = data;
    }

    public int getContextVersion() { return contextVersion; }
    public void setContextVersion(int contextVersion) { this.contextVersion = contextVersion; }

    public Map<String, Object> getTraceContext() { return traceContext; }
    public void setTraceContext(Map<String, Object> traceContext) { this.traceContext = traceContext; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
