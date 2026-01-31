package com.temporal.agentic.workflows;

import com.temporal.agentic.engine.*;
import com.temporal.agentic.models.*;
import io.temporal.workflow.*;
import org.slf4j.Logger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NonAgenticDagParentWorkflowImpl implements NonAgenticDagParentWorkflow {

    private static final Logger logger = Workflow.getLogger(NonAgenticDagParentWorkflowImpl.class);

    private String workflowStatus = "STARTED";
    private WorkflowContext context;
    private Map<String, String> nodeStatuses = new TreeMap<>();
    private DagSpec dagSpec;
    private DagExecutor dagExecutor;
    private ContextMergeEngine mergeEngine;

    @Override
    public FinalResponse executeWorkflow(InputRequest request, DagSpec dagSpec) {
        this.dagSpec = dagSpec;
        this.dagExecutor = new DagExecutor();
        this.mergeEngine = dagExecutor.getMergeEngine();

        // Initialize context
        context = new WorkflowContext();
        context.getTraceContext().put("workflowRunId", Workflow.getInfo().getWorkflowId());
        context.getTraceContext().put("requestId", request.getRequestId());

        Map<String, Object> initialData = new TreeMap<>();
        if (request.getData() != null) {
            initialData.putAll(request.getData());
        }
        context.setData(initialData);

        // Initialize node statuses
        for (NodeDefinition node : dagSpec.getNodes()) {
            nodeStatuses.put(node.getId(), "PENDING");
        }

        logger.info("Starting non-agentic workflow for request: {}", request.getRequestId());

        try {
            executeDAG();
            workflowStatus = "COMPLETED";
            return buildFinalResponse(request, "SUCCESS");
        } catch (Exception e) {
            logger.error("Workflow execution failed", e);
            workflowStatus = "FAILED";
            return buildFinalResponse(request, "FAILED");
        }
    }

    private void executeDAG() {
        Map<String, NodeDefinition> nodeMap = dagExecutor.buildNodeMap(dagSpec);
        List<String> executedNodes = new ArrayList<>();

        while (true) {
            DagExecutor.DagExecutionPlan plan = dagExecutor.planExecution(dagSpec, context);
            if (plan.getStages().isEmpty()) {
                break;
            }

            for (DagExecutor.DagExecutionPlan.Stage stage : plan.getStages()) {
                List<CompletableFuture<NodeExecutionResponse>> futures = new ArrayList<>();

                for (String nodeId : stage.getNodeIds()) {
                    nodeStatuses.put(nodeId, "RUNNING");
                    futures.add(executeNodeAsync(nodeId, nodeMap.get(nodeId)));
                }

                // Wait for all nodes in stage to complete
                Workflow.allOf(futures).get();

                // Collect and merge responses
                for (CompletableFuture<NodeExecutionResponse> future : futures) {
                    try {
                        NodeExecutionResponse response = future.get();
                        executedNodes.add(response.getNodeId());
                        mergeContextFromResponse(response);
                    } catch (Exception e) {
                        logger.error("Failed to get node response", e);
                    }
                }
            }
        }

        logger.info("DAG execution completed. Executed nodes: {}", executedNodes);
    }

    private CompletableFuture<NodeExecutionResponse> executeNodeAsync(String nodeId, NodeDefinition nodeDef) {
        return Workflow.async(() -> {
            try {
                NodeChildWorkflow child = Workflow.newChildWorkflowStub(NodeChildWorkflow.class,
                        ChildWorkflowOptions.newBuilder()
                                .setTaskQueue(nodeDef.getTaskQueue() != null ? nodeDef.getTaskQueue() : "default")
                                .setStartToCloseTimeout(Duration.ofSeconds(600))
                                .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL)
                                .build());

                ContextSnapshot snapshot = mergeEngine.captureContextSnapshot(context);
                Map<String, Object> nodeInputs = resolveNodeInputs(nodeDef, nodeMap(), context);

                NodeExecutionRequest childRequest = new NodeExecutionRequest(
                        Workflow.getInfo().getWorkflowId(),
                        nodeId,
                        nodeDef.getType(),
                        nodeInputs,
                        snapshot,
                        nodeId + "-" + Workflow.getInfo().getWorkflowId(),
                        1,
                        context.getTraceContext()
                );

                NodeExecutionResponse response = child.executeNode(childRequest);
                nodeStatuses.put(nodeId, response.getStatus());

                logger.info("Node {} completed with status: {}", nodeId, response.getStatus());
                return response;
            } catch (Exception e) {
                logger.error("Node {} execution error: {}", nodeId, e.getMessage());
                NodeExecutionResponse errorResponse = new NodeExecutionResponse();
                errorResponse.setNodeId(nodeId);
                errorResponse.setStatus("FAILED");
                errorResponse.setErrors(new ArrayList<>(List.of(e.getMessage())));
                return errorResponse;
            }
        });
    }

    private Map<String, NodeDefinition> nodeMap() {
        return dagExecutor.buildNodeMap(dagSpec);
    }

    private void mergeContextFromResponse(NodeExecutionResponse response) {
        if ("SUCCESS".equals(response.getStatus()) || "SKIPPED".equals(response.getStatus())) {
            mergeEngine.mergeContextDelta(context, response, System.currentTimeMillis());
        }
    }

    private Map<String, Object> resolveNodeInputs(NodeDefinition nodeDef, Map<String, NodeDefinition> nodeMap,
                                                  WorkflowContext context) {
        Map<String, Object> resolved = new TreeMap<>();
        if (nodeDef.getInputs() == null) {
            return resolved;
        }

        for (Map.Entry<String, Object> entry : nodeDef.getInputs().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String && ((String) value).startsWith("ref(")) {
                String refPath = ((String) value).substring(4, ((String) value).length() - 1);
                resolved.put(entry.getKey(), resolveContextPath(refPath, context));
            } else if ("required".equals(value)) {
                // Will be filled from input request
                resolved.put(entry.getKey(), null);
            } else {
                resolved.put(entry.getKey(), value);
            }
        }

        return resolved;
    }

    private Object resolveContextPath(String path, WorkflowContext context) {
        String[] parts = path.split("\\.");
        Object current = context.getData().getOrDefault("nodes", new TreeMap<>());

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private FinalResponse buildFinalResponse(InputRequest request, String status) {
        FinalResponse response = new FinalResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus(status);

        WorkflowStatus workflowStat = new WorkflowStatus();
        workflowStat.setStatus(workflowStatus);
        workflowStat.setTotalNodes(dagSpec.getNodes().size());
        long completed = nodeStatuses.values().stream().filter(s -> "SUCCESS".equals(s) || "SKIPPED".equals(s)).count();
        workflowStat.setCompletedNodes((int) completed);
        long failed = nodeStatuses.values().stream().filter(s -> "FAILED".equals(s)).count();
        workflowStat.setFailedNodes((int) failed);

        response.setWorkflowStatus(workflowStat);
        response.setOutputs(new TreeMap<>(context.getData()));
        response.setContextSummary(new TreeMap<>());
        response.getContextSummary().put("version", context.getVersion());
        response.getContextSummary().put("nodeCount", nodeStatuses.size());
        response.setErrors(new ArrayList<>());

        return response;
    }

    @Override
    public String getStatus() {
        return workflowStatus;
    }

    @Override
    public Map<String, String> getNodeStatuses() {
        return new TreeMap<>(nodeStatuses);
    }

    @Override
    public Map<String, Object> getContextSummary() {
        Map<String, Object> summary = new TreeMap<>();
        summary.put("contextVersion", context.getVersion());
        summary.put("nodeStates", context.getNodeStates().keySet());
        summary.put("status", workflowStatus);
        return summary;
    }
}
