package com.temporal.agentic.workflows;

import com.temporal.agentic.engine.*;
import com.temporal.agentic.models.*;
import io.temporal.workflow.*;
import org.slf4j.Logger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AgenticDagParentWorkflowImpl implements AgenticDagParentWorkflow {

    private static final Logger logger = Workflow.getLogger(AgenticDagParentWorkflowImpl.class);

    private String workflowStatus = "STARTED";
    private WorkflowContext context;
    private Map<String, String> nodeStatuses = new TreeMap<>();
    private DagSpec currentDagSpec;
    private DagExecutor dagExecutor;
    private ContextMergeEngine mergeEngine;
    private int currentDagVersion = 1;

    @Override
    public FinalResponse executeWorkflow(InputRequest request, DagSpec initialDagSpec) {
        this.currentDagSpec = initialDagSpec;
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
        // Add input parameters
        initialData.put("eventType", request.getEventType());
        initialData.put("channel", request.getChannel());
        initialData.put("locale", request.getLocale());
        context.setData(initialData);

        // Initialize node statuses
        for (NodeDefinition node : currentDagSpec.getNodes()) {
            nodeStatuses.put(node.getId(), "PENDING");
        }

        logger.info("Starting agentic workflow for request: {}", request.getRequestId());

        try {
            executeAgenticDAG(request);
            workflowStatus = "COMPLETED";
            return buildFinalResponse(request, "SUCCESS");
        } catch (Exception e) {
            logger.error("Workflow execution failed", e);
            workflowStatus = "FAILED";
            return buildFinalResponse(request, "FAILED");
        }
    }

    private void executeAgenticDAG(InputRequest request) {
        // First call the Planner node to optionally update DAG
        NodeDefinition plannerNode = currentDagSpec.getNodes().stream()
                .filter(n -> "PLANNER".equals(n.getType()))
                .findFirst()
                .orElse(null);

        if (plannerNode != null) {
            NodeExecutionResponse plannerResponse = executePlannerNode(plannerNode, request);
            mergeContextFromResponse(plannerResponse);
            nodeStatuses.put("planner", "SUCCESS");

            // If planner returned updated DAG spec, use it
            if (plannerResponse.getUpdatedDagSpec() != null) {
                DagSpec updatedSpec = plannerResponse.getUpdatedDagSpec();
                currentDagSpec = updatedSpec;
                currentDagVersion = updatedSpec.getVersion();
                logger.info("DAG updated by planner to version {}", currentDagVersion);

                // Update node statuses for new nodes
                for (NodeDefinition node : updatedSpec.getNodes()) {
                    nodeStatuses.putIfAbsent(node.getId(), "PENDING");
                }
            }
        }

        // Execute remaining nodes in DAG order
        Map<String, NodeDefinition> nodeMap = dagExecutor.buildNodeMap(currentDagSpec);
        Set<String> executedNodes = new HashSet<>();

        while (true) {
            DagExecutor.DagExecutionPlan plan = dagExecutor.planExecution(currentDagSpec, context);
            if (plan.getStages().isEmpty()) {
                break;
            }

            for (DagExecutor.DagExecutionPlan.Stage stage : plan.getStages()) {
                List<CompletableFuture<NodeExecutionResponse>> futures = new ArrayList<>();

                for (String nodeId : stage.getNodeIds()) {
                    if (!executedNodes.contains(nodeId)) {
                        nodeStatuses.put(nodeId, "RUNNING");
                        futures.add(executeNodeAsync(nodeId, nodeMap.get(nodeId)));
                    }
                }

                if (!futures.isEmpty()) {
                    Workflow.allOf(futures).get();

                    for (CompletableFuture<NodeExecutionResponse> future : futures) {
                        try {
                            NodeExecutionResponse response = future.get();
                            executedNodes.add(response.getNodeId());
                            mergeContextFromResponse(response);

                            // Check if this is a replan checkpoint
                            NodeDefinition nodeDef = nodeMap.get(response.getNodeId());
                            if (nodeDef != null && nodeDef.getPolicy() != null &&
                                    nodeDef.getPolicy().isReplanCheckpoint()) {
                                logger.info("Replan checkpoint reached at node {}", response.getNodeId());
                                // Could trigger replanner here if needed
                            }
                        } catch (Exception e) {
                            logger.error("Failed to get node response", e);
                        }
                    }
                }
            }
        }

        logger.info("Agentic DAG execution completed.");
    }

    private NodeExecutionResponse executePlannerNode(NodeDefinition plannerNode, InputRequest request) {
        return Workflow.newChildWorkflowStub(NodeChildWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setTaskQueue(plannerNode.getTaskQueue() != null ? plannerNode.getTaskQueue() : "default")
                        .setStartToCloseTimeout(Duration.ofSeconds(600))
                        .build()).executeNode(
                new NodeExecutionRequest(
                        Workflow.getInfo().getWorkflowId(),
                        "planner",
                        "PLANNER",
                        Map.of(
                                "eventType", request.getEventType(),
                                "channel", request.getChannel(),
                                "locale", request.getLocale()
                        ),
                        mergeEngine.captureContextSnapshot(context),
                        "planner-" + Workflow.getInfo().getWorkflowId(),
                        1,
                        context.getTraceContext()
                )
        );
    }

    private CompletableFuture<NodeExecutionResponse> executeNodeAsync(String nodeId, NodeDefinition nodeDef) {
        return Workflow.async(() -> {
            try {
                if ("PLANNER".equals(nodeDef.getType())) {
                    return null; // Skip planner in this phase
                }

                NodeChildWorkflow child = Workflow.newChildWorkflowStub(NodeChildWorkflow.class,
                        ChildWorkflowOptions.newBuilder()
                                .setTaskQueue(nodeDef.getTaskQueue() != null ? nodeDef.getTaskQueue() : "default")
                                .setStartToCloseTimeout(Duration.ofSeconds(600))
                                .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_REQUEST_CANCEL)
                                .build());

                ContextSnapshot snapshot = mergeEngine.captureContextSnapshot(context);
                Map<String, Object> nodeInputs = resolveNodeInputs(nodeDef, context);

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

    private void mergeContextFromResponse(NodeExecutionResponse response) {
        if (response != null && ("SUCCESS".equals(response.getStatus()) || "SKIPPED".equals(response.getStatus()))) {
            mergeEngine.mergeContextDelta(context, response, System.currentTimeMillis());
        }
    }

    private Map<String, Object> resolveNodeInputs(NodeDefinition nodeDef, WorkflowContext context) {
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
                resolved.put(entry.getKey(), context.getData().get(entry.getKey()));
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
        workflowStat.setTotalNodes(currentDagSpec.getNodes().size());
        long completed = nodeStatuses.values().stream().filter(s -> "SUCCESS".equals(s) || "SKIPPED".equals(s)).count();
        workflowStat.setCompletedNodes((int) completed);
        long failed = nodeStatuses.values().stream().filter(s -> "FAILED".equals(s)).count();
        workflowStat.setFailedNodes((int) failed);

        response.setWorkflowStatus(workflowStat);
        response.setOutputs(new TreeMap<>(context.getData()));
        response.setContextSummary(new TreeMap<>());
        response.getContextSummary().put("version", context.getVersion());
        response.getContextSummary().put("dagVersion", currentDagVersion);
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
        summary.put("dagVersion", currentDagVersion);
        summary.put("nodeStates", context.getNodeStates().keySet());
        summary.put("status", workflowStatus);
        return summary;
    }

    @Override
    public int getCurrentDagVersion() {
        return currentDagVersion;
    }
}
