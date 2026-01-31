package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.List;

/**
 * Final response from the parent workflow.
 */
public class FinalResponse implements Serializable {
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("status")
    private String status; // SUCCESS, FAILED

    @JsonProperty("workflowStatus")
    private WorkflowStatus workflowStatus;

    @JsonProperty("outputs")
    private Map<String, Object> outputs;

    @JsonProperty("contextSummary")
    private Map<String, Object> contextSummary;

    @JsonProperty("executionMetrics")
    private Map<String, Object> executionMetrics;

    @JsonProperty("errors")
    private List<String> errors;

    public FinalResponse() {}

    public FinalResponse(String requestId, String status, WorkflowStatus workflowStatus,
                        Map<String, Object> outputs, Map<String, Object> contextSummary,
                        Map<String, Object> executionMetrics, List<String> errors) {
        this.requestId = requestId;
        this.status = status;
        this.workflowStatus = workflowStatus;
        this.outputs = outputs;
        this.contextSummary = contextSummary;
        this.executionMetrics = executionMetrics;
        this.errors = errors;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public WorkflowStatus getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(WorkflowStatus workflowStatus) { this.workflowStatus = workflowStatus; }

    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }

    public Map<String, Object> getContextSummary() { return contextSummary; }
    public void setContextSummary(Map<String, Object> contextSummary) { this.contextSummary = contextSummary; }

    public Map<String, Object> getExecutionMetrics() { return executionMetrics; }
    public void setExecutionMetrics(Map<String, Object> executionMetrics) { this.executionMetrics = executionMetrics; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
