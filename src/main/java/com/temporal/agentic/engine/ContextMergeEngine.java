package com.temporal.agentic.engine;

import com.temporal.agentic.models.*;
import java.util.*;

/**
 * Implements deterministic merging of context deltas from child nodes.
 */
public class ContextMergeEngine {

    public void mergeContextDelta(WorkflowContext context, NodeExecutionResponse response,
                                  long completionTime) {
        ContextDelta delta = response.getContextDelta();
        if (delta == null) {
            return;
        }

        String nodeId = response.getNodeId();
        Map<String, Object> contextData = context.getData();

        // Namespace writes under context.nodes.<nodeId>
        Map<String, Object> nodeNamespace = (Map<String, Object>) contextData.computeIfAbsent(
                "nodes", k -> new TreeMap<>());
        Map<String, Object> nodeData = (Map<String, Object>) nodeNamespace.computeIfAbsent(
                nodeId, k -> new TreeMap<>());

        if (delta.getWrites() != null) {
            for (Map.Entry<String, Object> entry : delta.getWrites().entrySet()) {
                nodeData.put(entry.getKey(), entry.getValue());
            }
        }

        // Promote keys to top-level context.data
        if (delta.getPromoteKeys() != null) {
            for (String key : delta.getPromoteKeys()) {
                Object value = nodeData.get(key);
                if (value != null) {
                    contextData.put(key, value);
                }
            }
        }

        // Track node state
        WorkflowContext.NodeState nodeState = new WorkflowContext.NodeState(
                nodeId, response.getStatus(), response.getOutputs(), completionTime);
        context.getNodeStates().put(nodeId, nodeState);

        // Increment context version after merge
        context.incrementVersion();
    }

    public void applyMultipleDeltas(WorkflowContext context,
                                    List<NodeExecutionResponse> responses) {
        // Sort responses by completion order (deterministic: by nodeId if same timestamp)
        List<NodeExecutionResponse> sorted = new ArrayList<>(responses);
        sorted.sort((a, b) -> {
            long cmp = a.getNodeId().compareTo(b.getNodeId());
            return (int) cmp;
        });

        long timestamp = System.currentTimeMillis();
        for (NodeExecutionResponse response : sorted) {
            mergeContextDelta(context, response, timestamp++);
        }
    }

    public ContextSnapshot captureContextSnapshot(WorkflowContext context) {
        return new ContextSnapshot(
                context.getVersion(),
                new TreeMap<>(context.getTraceContext()),
                deepCopyMap(context.getData())
        );
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        Map<String, Object> copy = new TreeMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (entry.getValue() instanceof Map) {
                copy.put(entry.getKey(), deepCopyMap((Map<String, Object>) entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                copy.put(entry.getKey(), new ArrayList<>((List<?>) entry.getValue()));
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
}
