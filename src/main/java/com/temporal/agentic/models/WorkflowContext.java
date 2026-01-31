package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Versioned workflow context maintained by parent workflow.
 */
public class WorkflowContext implements Serializable {
    @JsonProperty("version")
    private int version;

    @JsonProperty("traceContext")
    private Map<String, Object> traceContext;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("nodeStates")
    private Map<String, NodeState> nodeStates;

    public WorkflowContext() {
        this.version = 0;
        this.traceContext = new TreeMap<>();
        this.data = new TreeMap<>();
        this.nodeStates = new TreeMap<>();
    }

    public WorkflowContext(int version, Map<String, Object> traceContext,
                          Map<String, Object> data, Map<String, NodeState> nodeStates) {
        this.version = version;
        this.traceContext = traceContext;
        this.data = data;
        this.nodeStates = nodeStates;
    }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Map<String, Object> getTraceContext() { return traceContext; }
    public void setTraceContext(Map<String, Object> traceContext) { this.traceContext = traceContext; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public Map<String, NodeState> getNodeStates() { return nodeStates; }
    public void setNodeStates(Map<String, NodeState> nodeStates) { this.nodeStates = nodeStates; }

    public WorkflowContext incrementVersion() {
        this.version++;
        return this;
    }

    public static class NodeState implements Serializable {
        @JsonProperty("nodeId")
        private String nodeId;

        @JsonProperty("status")
        private String status; // PENDING, RUNNING, SUCCESS, FAILED, SKIPPED

        @JsonProperty("outputs")
        private Map<String, Object> outputs;

        @JsonProperty("completionTime")
        private long completionTime;

        public NodeState() {}

        public NodeState(String nodeId, String status, Map<String, Object> outputs, long completionTime) {
            this.nodeId = nodeId;
            this.status = status;
            this.outputs = outputs;
            this.completionTime = completionTime;
        }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Map<String, Object> getOutputs() { return outputs; }
        public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }

        public long getCompletionTime() { return completionTime; }
        public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
    }
}
