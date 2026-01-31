package com.temporal.agentic.workflows;

import com.temporal.agentic.models.*;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.Map;

@WorkflowInterface
public interface NonAgenticDagParentWorkflow {

    @WorkflowMethod
    FinalResponse executeWorkflow(InputRequest request, DagSpec dagSpec);

    @QueryMethod
    String getStatus();

    @QueryMethod
    Map<String, String> getNodeStatuses();

    @QueryMethod
    Map<String, Object> getContextSummary();
}
