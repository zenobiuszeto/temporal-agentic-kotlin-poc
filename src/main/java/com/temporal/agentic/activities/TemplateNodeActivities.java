package com.temporal.agentic.activities;

import com.temporal.agentic.models.*;
import java.util.*;

public interface TemplateNodeActivities {

    NodeExecutionResponse executeValidate(NodeExecutionRequest request);

    NodeExecutionResponse executeGovernance(NodeExecutionRequest request);

    NodeExecutionResponse executeRetrieve(NodeExecutionRequest request);

    NodeExecutionResponse executeTemplateMap(NodeExecutionRequest request);

    NodeExecutionResponse executeTransform(NodeExecutionRequest request);

    NodeExecutionResponse executeDecide(NodeExecutionRequest request);

    NodeExecutionResponse executeAction(NodeExecutionRequest request);

    NodeExecutionResponse executeFinalize(NodeExecutionRequest request);

    NodeExecutionResponse executePlanner(NodeExecutionRequest request);
}
