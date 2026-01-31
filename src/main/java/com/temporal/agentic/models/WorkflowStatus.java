package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Workflow execution status.
 */
public class WorkflowStatus implements Serializable {
    @JsonProperty("status")
    private String status; // RUNNING, COMPLETED, FAILED

    @JsonProperty("completedNodes")
    private int completedNodes;

    @JsonProperty("totalNodes")
    private int totalNodes;

    @JsonProperty("failedNodes")
    private int failedNodes;

    public WorkflowStatus() {}

    public WorkflowStatus(String status, int completedNodes, int totalNodes, int failedNodes) {
        this.status = status;
        this.completedNodes = completedNodes;
        this.totalNodes = totalNodes;
        this.failedNodes = failedNodes;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCompletedNodes() { return completedNodes; }
    public void setCompletedNodes(int completedNodes) { this.completedNodes = completedNodes; }

    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }

    public int getFailedNodes() { return failedNodes; }
    public void setFailedNodes(int failedNodes) { this.failedNodes = failedNodes; }
}
