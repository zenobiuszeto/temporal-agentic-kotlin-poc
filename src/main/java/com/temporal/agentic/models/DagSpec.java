package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Workflow DAG specification.
 */
public class DagSpec implements Serializable {
    @JsonProperty("workflowId")
    private String workflowId;

    @JsonProperty("version")
    private int version;

    @JsonProperty("maxParallelNodes")
    private int maxParallelNodes;

    @JsonProperty("mergeStrategy")
    private String mergeStrategy; // DETERMINISTIC, LAST_WRITE_WINS

    @JsonProperty("failureStrategy")
    private String failureStrategy; // FAIL_FAST, CONTINUE

    @JsonProperty("nodes")
    private List<NodeDefinition> nodes;

    @JsonProperty("edges")
    private List<EdgeDefinition> edges;

    @JsonProperty("executionSettings")
    private Map<String, Object> executionSettings;

    public DagSpec() {}

    public DagSpec(String workflowId, int version, int maxParallelNodes,
                  String mergeStrategy, String failureStrategy,
                  List<NodeDefinition> nodes, List<EdgeDefinition> edges,
                  Map<String, Object> executionSettings) {
        this.workflowId = workflowId;
        this.version = version;
        this.maxParallelNodes = maxParallelNodes;
        this.mergeStrategy = mergeStrategy;
        this.failureStrategy = failureStrategy;
        this.nodes = nodes;
        this.edges = edges;
        this.executionSettings = executionSettings;
    }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public int getMaxParallelNodes() { return maxParallelNodes; }
    public void setMaxParallelNodes(int maxParallelNodes) { this.maxParallelNodes = maxParallelNodes; }

    public String getMergeStrategy() { return mergeStrategy; }
    public void setMergeStrategy(String mergeStrategy) { this.mergeStrategy = mergeStrategy; }

    public String getFailureStrategy() { return failureStrategy; }
    public void setFailureStrategy(String failureStrategy) { this.failureStrategy = failureStrategy; }

    public List<NodeDefinition> getNodes() { return nodes; }
    public void setNodes(List<NodeDefinition> nodes) { this.nodes = nodes; }

    public List<EdgeDefinition> getEdges() { return edges; }
    public void setEdges(List<EdgeDefinition> edges) { this.edges = edges; }

    public Map<String, Object> getExecutionSettings() { return executionSettings; }
    public void setExecutionSettings(Map<String, Object> executionSettings) { this.executionSettings = executionSettings; }
}
