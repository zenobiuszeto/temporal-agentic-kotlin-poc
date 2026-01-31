package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.List;

/**
 * Response from child node workflows.
 */
public class NodeExecutionResponse implements Serializable {
    @JsonProperty("nodeId")
    private String nodeId;

    @JsonProperty("status")
    private String status; // SUCCESS, FAILED, SKIPPED

    @JsonProperty("outputs")
    private Map<String, Object> outputs;

    @JsonProperty("contextDelta")
    private ContextDelta contextDelta;

    @JsonProperty("errors")
    private List<String> errors;

    @JsonProperty("metrics")
    private Map<String, Object> metrics;

    @JsonProperty("updatedDagSpec")
    private DagSpec updatedDagSpec; // For agentic planner node only

    public NodeExecutionResponse() {}

    public NodeExecutionResponse(String nodeId, String status, Map<String, Object> outputs,
                               ContextDelta contextDelta, List<String> errors,
                               Map<String, Object> metrics, DagSpec updatedDagSpec) {
        this.nodeId = nodeId;
        this.status = status;
        this.outputs = outputs;
        this.contextDelta = contextDelta;
        this.errors = errors;
        this.metrics = metrics;
        this.updatedDagSpec = updatedDagSpec;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }

    public ContextDelta getContextDelta() { return contextDelta; }
    public void setContextDelta(ContextDelta contextDelta) { this.contextDelta = contextDelta; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public DagSpec getUpdatedDagSpec() { return updatedDagSpec; }
    public void setUpdatedDagSpec(DagSpec updatedDagSpec) { this.updatedDagSpec = updatedDagSpec; }
}
