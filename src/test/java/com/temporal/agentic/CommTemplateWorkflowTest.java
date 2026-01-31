package com.temporal.agentic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporal.agentic.engine.ContextMergeEngine;
import com.temporal.agentic.engine.DagExecutor;
import com.temporal.agentic.models.*;
import com.temporal.agentic.workflows.*;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import com.temporal.agentic.activities.TemplateNodeActivitiesImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive end-to-end tests for communication template mapping workflow.
 */
public class CommTemplateWorkflowTest {
    private static final Logger logger = LoggerFactory.getLogger(CommTemplateWorkflowTest.class);

    private TestWorkflowEnvironment testEnv;
    private NonAgenticDagParentWorkflow nonAgenticWorkflow;
    private AgenticDagParentWorkflow agenticWorkflow;
    private DagSpec nonAgenticSpec;
    private DagSpec agenticSpec;

    @BeforeEach
    public void setUp() throws Exception {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("comm_template_tasks");

        // Register workflows and activities
        worker.registerWorkflowImplementationTypes(
                NonAgenticDagParentWorkflowImpl.class,
                AgenticDagParentWorkflowImpl.class,
                NodeChildWorkflowImpl.class
        );
        worker.registerActivitiesImpl(new TemplateNodeActivitiesImpl());

        testEnv.start();

        // Load specs
        loadSpecs();
    }

    private void loadSpecs() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        InputStream nonAgenticInputStream = getClass().getClassLoader()
                .getResourceAsStream("specs/non_agentic_comm_template.json");
        nonAgenticSpec = mapper.readValue(nonAgenticInputStream, DagSpec.class);

        InputStream agenticInputStream = getClass().getClassLoader()
                .getResourceAsStream("specs/agentic_comm_template.json");
        agenticSpec = mapper.readValue(agenticInputStream, DagSpec.class);
    }

    @Test
    public void testNonAgenticWorkflowEndToEnd() {
        logger.info("Starting non-agentic workflow test");

        InputRequest request = new InputRequest();
        request.setRequestId("req-001");
        request.setWorkflowId("comm_template_nonagentic");
        request.setEventType("STATEMENT_READY");
        request.setChannel("EMAIL");
        request.setLocale("en_US");
        request.setData(new TreeMap<>());

        nonAgenticWorkflow = testEnv.newWorkflowStub(NonAgenticDagParentWorkflow.class);
        FinalResponse response = nonAgenticWorkflow.executeWorkflow(request, nonAgenticSpec);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("req-001", response.getRequestId());
        assertNotNull(response.getWorkflowStatus());
        assertEquals("COMPLETED", response.getWorkflowStatus().getStatus());

        // Verify all critical nodes executed
        Map<String, String> nodeStatuses = nonAgenticWorkflow.getNodeStatuses();
        assertEquals("SUCCESS", nodeStatuses.get("validate"));
        assertEquals("SUCCESS", nodeStatuses.get("governance"));
        assertEquals("SUCCESS", nodeStatuses.get("retrieve"));
        assertEquals("SUCCESS", nodeStatuses.get("template_map"));
        assertEquals("SUCCESS", nodeStatuses.get("finalize"));

        // Check context was properly merged
        Map<String, Object> contextSummary = nonAgenticWorkflow.getContextSummary();
        assertNotNull(contextSummary);
        assertTrue(contextSummary.containsKey("version"));
        assertTrue((int) contextSummary.get("version") > 0);

        logger.info("Non-agentic workflow test PASSED");
    }

    @Test
    public void testAgenticWorkflowEndToEnd() {
        logger.info("Starting agentic workflow test");

        InputRequest request = new InputRequest();
        request.setRequestId("req-002");
        request.setWorkflowId("comm_template_agentic");
        request.setEventType("FRAUD_ALERT");
        request.setChannel("SMS");
        request.setLocale("en_US");
        request.setData(new TreeMap<>());

        agenticWorkflow = testEnv.newWorkflowStub(AgenticDagParentWorkflow.class);
        FinalResponse response = agenticWorkflow.executeWorkflow(request, agenticSpec);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("req-002", response.getRequestId());

        // Verify planner was executed
        Map<String, String> nodeStatuses = agenticWorkflow.getNodeStatuses();
        assertEquals("SUCCESS", nodeStatuses.get("planner"));

        // Verify DAG version exists
        int dagVersion = agenticWorkflow.getCurrentDagVersion();
        assertTrue(dagVersion >= 1);

        logger.info("Agentic workflow test PASSED");
    }

    @Test
    public void testContextPropagationAndMerging() throws Exception {
        logger.info("Starting context propagation test");

        // Create a context and verify deterministic merging
        WorkflowContext context = new WorkflowContext();
        context.setVersion(0);
        context.getTraceContext().put("requestId", "test-001");

        Map<String, Object> initialData = new TreeMap<>();
        initialData.put("channel", "EMAIL");
        context.setData(initialData);

        // Create multiple context deltas
        List<NodeExecutionResponse> responses = new ArrayList<>();

        // Response from VALIDATE node
        NodeExecutionResponse validateResponse = new NodeExecutionResponse();
        validateResponse.setNodeId("validate");
        validateResponse.setStatus("SUCCESS");
        ContextDelta validateDelta = new ContextDelta();
        validateDelta.setNodeId("validate");
        validateDelta.setWrites(Map.of("isValid", true, "validationErrors", new ArrayList<>()));
        validateResponse.setContextDelta(validateDelta);
        responses.add(validateResponse);

        // Response from GOVERNANCE node
        NodeExecutionResponse govResponse = new NodeExecutionResponse();
        govResponse.setNodeId("governance");
        govResponse.setStatus("SUCCESS");
        ContextDelta govDelta = new ContextDelta();
        govDelta.setNodeId("governance");
        govDelta.setWrites(Map.of("disclaimers", List.of("Reg E notice")));
        govResponse.setContextDelta(govDelta);
        responses.add(govResponse);

        // Merge all deltas
        ContextMergeEngine mergeEngine = new ContextMergeEngine();
        mergeEngine.applyMultipleDeltas(context, responses);

        // Verify context was merged deterministically
        assertEquals(2, context.getVersion()); // Version incremented twice
        assertNotNull(context.getData().get("nodes"));

        Map<String, Object> nodeData = (Map<String, Object>) context.getData().get("nodes");
        assertNotNull(nodeData.get("validate"));
        assertNotNull(nodeData.get("governance"));

        logger.info("Context propagation test PASSED");
    }

    @Test
    public void testDagTopologyComputation() throws Exception {
        logger.info("Starting DAG topology test");

        DagExecutor executor = new DagExecutor();
        DagExecutor.DagExecutionPlan plan = executor.planExecution(nonAgenticSpec, new WorkflowContext());

        assertNotNull(plan);
        assertTrue(plan.getStages().size() > 0);

        // Verify stages are properly ordered
        for (int i = 0; i < plan.getStages().size(); i++) {
            DagExecutor.DagExecutionPlan.Stage stage = plan.getStages().get(i);
            assertEquals(i, stage.getStageNumber());
            assertFalse(stage.getNodeIds().isEmpty());
        }

        logger.info("DAG topology test PASSED. Total stages: {}", plan.getStages().size());
    }

    @Test
    public void testBranchingWithConditions() throws Exception {
        logger.info("Starting branching test");

        InputRequest request = new InputRequest();
        request.setRequestId("req-003");
        request.setWorkflowId("comm_template_nonagentic");
        request.setEventType("FRAUD_ALERT");
        request.setChannel("EMAIL");
        request.setLocale("en_US");
        request.setData(new TreeMap<>());

        nonAgenticWorkflow = testEnv.newWorkflowStub(NonAgenticDagParentWorkflow.class);
        FinalResponse response = nonAgenticWorkflow.executeWorkflow(request, nonAgenticSpec);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        // Verify all governance checks were run (since it's FRAUD_ALERT)
        Map<String, String> nodeStatuses = nonAgenticWorkflow.getNodeStatuses();
        assertEquals("SUCCESS", nodeStatuses.get("governance"));

        logger.info("Branching test PASSED");
    }

    @Test
    public void testDeterministicOrdering() throws Exception {
        logger.info("Starting deterministic ordering test");

        // Execute workflow multiple times and verify consistent results
        List<FinalResponse> responses = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            InputRequest request = new InputRequest();
            request.setRequestId("req-determ-" + i);
            request.setWorkflowId("comm_template_nonagentic");
            request.setEventType("STATEMENT_READY");
            request.setChannel("EMAIL");
            request.setLocale("en_US");
            request.setData(new TreeMap<>());

            nonAgenticWorkflow = testEnv.newWorkflowStub(NonAgenticDagParentWorkflow.class);
            FinalResponse response = nonAgenticWorkflow.executeWorkflow(request, nonAgenticSpec);
            responses.add(response);
        }

        // Both responses should have same structure
        assertEquals(responses.get(0).getStatus(), responses.get(1).getStatus());
        assertEquals(responses.get(0).getWorkflowStatus().getCompletedNodes(),
                responses.get(1).getWorkflowStatus().getCompletedNodes());

        logger.info("Deterministic ordering test PASSED");
    }

    @Test
    public void testContextSnapshotCapture() throws Exception {
        logger.info("Starting context snapshot test");

        WorkflowContext context = new WorkflowContext();
        context.setVersion(5);
        context.getTraceContext().put("correlationId", "corr-123");
        context.getData().put("testKey", "testValue");

        ContextMergeEngine mergeEngine = new ContextMergeEngine();
        ContextSnapshot snapshot = mergeEngine.captureContextSnapshot(context);

        assertNotNull(snapshot);
        assertEquals(5, snapshot.getContextVersion());
        assertEquals("corr-123", snapshot.getTraceContext().get("correlationId"));
        assertEquals("testValue", snapshot.getData().get("testKey"));

        logger.info("Context snapshot test PASSED");
    }

    public static void cleanUp(TestWorkflowEnvironment testEnv) {
        if (testEnv != null) {
            testEnv.close();
        }
    }
}
