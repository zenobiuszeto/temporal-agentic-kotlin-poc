package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.List;

/**
 * Context delta returned from child workflows to be merged into parent context.
 */
public class ContextDelta implements Serializable {
    @JsonProperty("nodeId")
    private String nodeId;

    @JsonProperty("writes")
    private Map<String, Object> writes; // Namespaced writes default to context.nodes.<nodeId>.*

    @JsonProperty("promoteKeys")
    private List<String> promoteKeys; // Keys to lift to top-level context.data.*

    @JsonProperty("timestamp")
    private long timestamp;

    public ContextDelta() {}

    public ContextDelta(String nodeId, Map<String, Object> writes, List<String> promoteKeys, long timestamp) {
        this.nodeId = nodeId;
        this.writes = writes;
        this.promoteKeys = promoteKeys;
        this.timestamp = timestamp;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Map<String, Object> getWrites() { return writes; }
    public void setWrites(Map<String, Object> writes) { this.writes = writes; }

    public List<String> getPromoteKeys() { return promoteKeys; }
    public void setPromoteKeys(List<String> promoteKeys) { this.promoteKeys = promoteKeys; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
