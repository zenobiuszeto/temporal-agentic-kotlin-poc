package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.List;

/**
 * Request passed to child node workflows.
 */
public class NodeExecutionRequest implements Serializable {
    @JsonProperty("workflowRunId")
    private String workflowRunId;

    @JsonProperty("nodeId")
    private String nodeId;

    @JsonProperty("nodeType")
    private String nodeType;

    @JsonProperty("nodeInput")
    private Map<String, Object> nodeInput;

    @JsonProperty("contextSnapshot")
    private ContextSnapshot contextSnapshot;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("attempt")
    private int attempt;

    @JsonProperty("traceContext")
    private Map<String, Object> traceContext;

    public NodeExecutionRequest() {}

    public NodeExecutionRequest(String workflowRunId, String nodeId, String nodeType,
                               Map<String, Object> nodeInput, ContextSnapshot contextSnapshot,
                               String idempotencyKey, int attempt, Map<String, Object> traceContext) {
        this.workflowRunId = workflowRunId;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.nodeInput = nodeInput;
        this.contextSnapshot = contextSnapshot;
        this.idempotencyKey = idempotencyKey;
        this.attempt = attempt;
        this.traceContext = traceContext;
    }

    public String getWorkflowRunId() { return workflowRunId; }
    public void setWorkflowRunId(String workflowRunId) { this.workflowRunId = workflowRunId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public Map<String, Object> getNodeInput() { return nodeInput; }
    public void setNodeInput(Map<String, Object> nodeInput) { this.nodeInput = nodeInput; }

    public ContextSnapshot getContextSnapshot() { return contextSnapshot; }
    public void setContextSnapshot(ContextSnapshot contextSnapshot) { this.contextSnapshot = contextSnapshot; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }

    public Map<String, Object> getTraceContext() { return traceContext; }
    public void setTraceContext(Map<String, Object> traceContext) { this.traceContext = traceContext; }
}
