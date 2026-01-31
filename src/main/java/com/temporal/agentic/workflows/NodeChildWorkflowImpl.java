package com.temporal.agentic.workflows;

import com.temporal.agentic.models.*;
import com.temporal.agentic.activities.TemplateNodeActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.TreeMap;

public class NodeChildWorkflowImpl implements NodeChildWorkflow {

    private static final Logger logger = Workflow.getLogger(NodeChildWorkflowImpl.class);

    private final TemplateNodeActivities activities =
            Workflow.newActivityStub(TemplateNodeActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(120))
                            .setRetryPolicy(io.temporal.common.RetryOptions.newBuilder()
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setMaximumAttempts(2)
                                    .build())
                            .build());

    @Override
    public NodeExecutionResponse executeNode(NodeExecutionRequest request) {
        String nodeType = request.getNodeType();
        String nodeId = request.getNodeId();

        logger.info("Executing node {} of type {}", nodeId, nodeType);

        try {
            NodeExecutionResponse response = routeNodeExecution(request, nodeType);
            return response;
        } catch (Exception e) {
            logger.error("Node execution failed: " + nodeId, e);
            return createFailedResponse(request, e.getMessage());
        }
    }

    private NodeExecutionResponse routeNodeExecution(NodeExecutionRequest request, String nodeType) {
        switch (nodeType.toUpperCase()) {
            case "VALIDATE":
                return activities.executeValidate(request);
            case "GOVERNANCE":
                return activities.executeGovernance(request);
            case "RETRIEVE":
                return activities.executeRetrieve(request);
            case "TEMPLATE_MAP":
                return activities.executeTemplateMap(request);
            case "TRANSFORM":
                return activities.executeTransform(request);
            case "DECIDE":
                return activities.executeDecide(request);
            case "ACTION":
                return activities.executeAction(request);
            case "FINALIZE":
                return activities.executeFinalize(request);
            case "PLANNER":
                return activities.executePlanner(request);
            default:
                throw new IllegalArgumentException("Unknown node type: " + nodeType);
        }
    }

    private NodeExecutionResponse createFailedResponse(NodeExecutionRequest request, String errorMessage) {
        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(request.getNodeId());
        response.setStatus("FAILED");
        response.setOutputs(new TreeMap<>());
        response.setErrors(new ArrayList<>());
        response.getErrors().add(errorMessage);
        response.setMetrics(new TreeMap<>());
        return response;
    }
}
