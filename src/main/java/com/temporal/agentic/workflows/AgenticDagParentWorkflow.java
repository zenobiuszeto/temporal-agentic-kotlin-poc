package com.temporal.agentic.workflows;

import com.temporal.agentic.models.*;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.Map;

@WorkflowInterface
public interface AgenticDagParentWorkflow {

    @WorkflowMethod
    FinalResponse executeWorkflow(InputRequest request, DagSpec initialDagSpec);

    @QueryMethod
    String getStatus();

    @QueryMethod
    Map<String, String> getNodeStatuses();

    @QueryMethod
    Map<String, Object> getContextSummary();

    @QueryMethod
    int getCurrentDagVersion();
}
