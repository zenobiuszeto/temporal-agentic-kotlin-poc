package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * Node definition in DAG spec.
 */
public class NodeDefinition implements Serializable {
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type; // PLANNER, VALIDATE, GOVERNANCE, RETRIEVE, TRANSFORM, DECIDE, TEMPLATE_MAP, ACTION, FINALIZE

    @JsonProperty("name")
    private String name;

    @JsonProperty("inputs")
    private Map<String, Object> inputs;

    @JsonProperty("outputs")
    private Map<String, Object> outputs;

    @JsonProperty("taskQueue")
    private String taskQueue;

    @JsonProperty("policy")
    private NodePolicy policy;

    @JsonProperty("timeouts")
    private TimeoutSpec timeouts;

    @JsonProperty("retryPolicy")
    private RetrySpec retryPolicy;

    @JsonProperty("compensationNodeId")
    private String compensationNodeId;

    @JsonProperty("condition")
    private String condition;

    public NodeDefinition() {}

    public NodeDefinition(String id, String type, String name, Map<String, Object> inputs,
                         Map<String, Object> outputs, String taskQueue, NodePolicy policy,
                         TimeoutSpec timeouts, RetrySpec retryPolicy,
                         String compensationNodeId, String condition) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.taskQueue = taskQueue;
        this.policy = policy;
        this.timeouts = timeouts;
        this.retryPolicy = retryPolicy;
        this.compensationNodeId = compensationNodeId;
        this.condition = condition;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Object> getInputs() { return inputs; }
    public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }

    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }

    public String getTaskQueue() { return taskQueue; }
    public void setTaskQueue(String taskQueue) { this.taskQueue = taskQueue; }

    public NodePolicy getPolicy() { return policy; }
    public void setPolicy(NodePolicy policy) { this.policy = policy; }

    public TimeoutSpec getTimeouts() { return timeouts; }
    public void setTimeouts(TimeoutSpec timeouts) { this.timeouts = timeouts; }

    public RetrySpec getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(RetrySpec retryPolicy) { this.retryPolicy = retryPolicy; }

    public String getCompensationNodeId() { return compensationNodeId; }
    public void setCompensationNodeId(String compensationNodeId) { this.compensationNodeId = compensationNodeId; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
