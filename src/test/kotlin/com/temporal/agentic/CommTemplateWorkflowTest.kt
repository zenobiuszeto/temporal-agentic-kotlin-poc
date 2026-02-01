package com.temporal.agentic

import com.fasterxml.jackson.databind.ObjectMapper
import com.temporal.agentic.activities.TemplateNodeActivitiesImpl
import com.temporal.agentic.engine.ContextMergeEngine
import com.temporal.agentic.engine.DagExecutor
import com.temporal.agentic.models.*
import com.temporal.agentic.workflows.*
import io.temporal.testing.TestWorkflowEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommTemplateWorkflowTest {
    
    private val logger: Logger = LoggerFactory.getLogger(CommTemplateWorkflowTest::class.java)
    
    private lateinit var testEnv: TestWorkflowEnvironment
    private lateinit var nonAgenticWorkflow: NonAgenticDagParentWorkflow
    private lateinit var agenticWorkflow: AgenticDagParentWorkflow
    private lateinit var nonAgenticSpec: DagSpec
    private lateinit var agenticSpec: DagSpec
    
    @BeforeEach
    fun setUp() {
        testEnv = TestWorkflowEnvironment.newInstance()
        val worker = testEnv.newWorker("comm_template_tasks")
        
        worker.registerWorkflowImplementationTypes(
            NonAgenticDagParentWorkflowImpl::class.java,
            AgenticDagParentWorkflowImpl::class.java,
            NodeChildWorkflowImpl::class.java
        )
        worker.registerActivitiesImplementations(TemplateNodeActivitiesImpl())
        
        testEnv.start()
        
        loadSpecs()
    }
    
    private fun loadSpecs() {
        val mapper = ObjectMapper().apply {
            findAndRegisterModules() // This registers the Kotlin module
        }
        
        val nonAgenticInputStream = javaClass.classLoader
            .getResourceAsStream("specs/non_agentic_comm_template.json")
            ?: throw IllegalStateException("Could not load non_agentic_comm_template.json")
        nonAgenticSpec = mapper.readValue(nonAgenticInputStream, DagSpec::class.java)
        
        val agenticInputStream = javaClass.classLoader
            .getResourceAsStream("specs/agentic_comm_template.json")
            ?: throw IllegalStateException("Could not load agentic_comm_template.json")
        agenticSpec = mapper.readValue(agenticInputStream, DagSpec::class.java)
    }
    
    @Test
    fun testNonAgenticWorkflowEndToEnd() {
        logger.info("Starting non-agentic workflow test")
        
        val request = InputRequest(
            requestId = "req-001",
            workflowId = "comm_template_nonagentic",
            eventType = "STATEMENT_READY",
            channel = "EMAIL",
            locale = "en_US",
            data = TreeMap()
        )
        
        nonAgenticWorkflow = testEnv.workflowClient.newWorkflowStub(
            NonAgenticDagParentWorkflow::class.java,
            io.temporal.client.WorkflowOptions.newBuilder()
                .setTaskQueue("comm_template_tasks")
                .build()
        )
        val response = nonAgenticWorkflow.executeWorkflow(request, nonAgenticSpec)
        
        assertNotNull(response)
        assertEquals("SUCCESS", response.status)
        assertEquals("req-001", response.requestId)
        assertNotNull(response.workflowStatus)
        assertEquals("COMPLETED", response.workflowStatus?.status)
        
        val nodeStatuses = nonAgenticWorkflow.getNodeStatuses()
        assertEquals("SUCCESS", nodeStatuses["validate"])
        assertEquals("SUCCESS", nodeStatuses["governance"])
        assertEquals("SUCCESS", nodeStatuses["retrieve"])
        assertEquals("SUCCESS", nodeStatuses["template_map"])
        assertEquals("SUCCESS", nodeStatuses["finalize"])
        
        val contextSummary = nonAgenticWorkflow.getContextSummary()
        assertNotNull(contextSummary)
        assertTrue(contextSummary.containsKey("contextVersion"))
        assertTrue((contextSummary["contextVersion"] as Int) > 0)
        
        logger.info("Non-agentic workflow test PASSED")
    }
    
    @Test
    fun testAgenticWorkflowEndToEnd() {
        logger.info("Starting agentic workflow test")
        
        val request = InputRequest(
            requestId = "req-002",
            workflowId = "comm_template_agentic",
            eventType = "FRAUD_ALERT",
            channel = "SMS",
            locale = "en_US",
            data = TreeMap()
        )
        
        agenticWorkflow = testEnv.workflowClient.newWorkflowStub(
            AgenticDagParentWorkflow::class.java,
            io.temporal.client.WorkflowOptions.newBuilder()
                .setTaskQueue("comm_template_tasks")
                .build()
        )
        val response = agenticWorkflow.executeWorkflow(request, agenticSpec)
        
        assertNotNull(response)
        assertEquals("SUCCESS", response.status)
        assertEquals("req-002", response.requestId)
        
        val nodeStatuses = agenticWorkflow.getNodeStatuses()
        assertEquals("SUCCESS", nodeStatuses["planner"])
        
        val dagVersion = agenticWorkflow.getCurrentDagVersion()
        assertTrue(dagVersion >= 1)
        
        logger.info("Agentic workflow test PASSED")
    }
    
    @Test
    fun testContextPropagationAndMerging() {
        logger.info("Starting context propagation test")
        
        val context = WorkflowContext(
            version = 0,
            traceContext = TreeMap<String, Any>().apply { put("requestId", "test-001") },
            data = TreeMap<String, Any>().apply { put("channel", "EMAIL") }
        )
        
        val responses = mutableListOf<NodeExecutionResponse>()
        
        val validateResponse = NodeExecutionResponse(
            nodeId = "validate",
            status = "SUCCESS",
            contextDelta = ContextDelta(
                nodeId = "validate",
                writes = mapOf("isValid" to true, "validationErrors" to emptyList<String>())
            )
        )
        responses.add(validateResponse)
        
        val govResponse = NodeExecutionResponse(
            nodeId = "governance",
            status = "SUCCESS",
            contextDelta = ContextDelta(
                nodeId = "governance",
                writes = mapOf("disclaimers" to listOf("Reg E notice"))
            )
        )
        responses.add(govResponse)
        
        val mergeEngine = ContextMergeEngine()
        mergeEngine.applyMultipleDeltas(context, responses)
        
        assertEquals(2, context.version)
        assertNotNull(context.data["nodes"])
        
        val nodeData = context.data["nodes"] as Map<*, *>
        assertNotNull(nodeData["validate"])
        assertNotNull(nodeData["governance"])
        
        logger.info("Context propagation test PASSED")
    }
    
    @Test
    fun testDagTopologyComputation() {
        logger.info("Starting DAG topology test")
        
        val executor = DagExecutor()
        val plan = executor.planExecution(nonAgenticSpec, WorkflowContext())
        
        assertNotNull(plan)
        assertTrue(plan.stages.size > 0)
        
        for ((i, stage) in plan.stages.withIndex()) {
            assertEquals(i, stage.stageNumber)
            assertTrue(stage.nodeIds.isNotEmpty())
        }
        
        logger.info("DAG topology test PASSED. Total stages: ${plan.stages.size}")
    }
    
    @Test
    fun testBranchingWithConditions() {
        logger.info("Starting branching test")
        
        val request = InputRequest(
            requestId = "req-003",
            workflowId = "comm_template_nonagentic",
            eventType = "FRAUD_ALERT",
            channel = "EMAIL",
            locale = "en_US",
            data = TreeMap()
        )
        
        nonAgenticWorkflow = testEnv.workflowClient.newWorkflowStub(
            NonAgenticDagParentWorkflow::class.java,
            io.temporal.client.WorkflowOptions.newBuilder()
                .setTaskQueue("comm_template_tasks")
                .build()
        )
        val response = nonAgenticWorkflow.executeWorkflow(request, nonAgenticSpec)
        
        assertNotNull(response)
        assertEquals("SUCCESS", response.status)
        
        val nodeStatuses = nonAgenticWorkflow.getNodeStatuses()
        assertEquals("SUCCESS", nodeStatuses["governance"])
        
        logger.info("Branching test PASSED")
    }
    
    @Test
    fun testDeterministicOrdering() {
        logger.info("Starting deterministic ordering test")
        
        val responses = mutableListOf<FinalResponse>()
        
        for (i in 0 until 2) {
            val request = InputRequest(
                requestId = "req-determ-$i",
                workflowId = "comm_template_nonagentic",
                eventType = "STATEMENT_READY",
                channel = "EMAIL",
                locale = "en_US",
                data = TreeMap()
            )
            
            nonAgenticWorkflow = testEnv.workflowClient.newWorkflowStub(
                NonAgenticDagParentWorkflow::class.java,
                io.temporal.client.WorkflowOptions.newBuilder()
                    .setTaskQueue("comm_template_tasks")
                    .build()
            )
            val response = nonAgenticWorkflow.executeWorkflow(request, nonAgenticSpec)
            responses.add(response)
        }
        
        assertEquals(responses[0].status, responses[1].status)
        assertEquals(
            responses[0].workflowStatus?.completedNodes,
            responses[1].workflowStatus?.completedNodes
        )
        
        logger.info("Deterministic ordering test PASSED")
    }
    
    @Test
    fun testContextSnapshotCapture() {
        logger.info("Starting context snapshot test")
        
        val context = WorkflowContext(
            version = 5,
            traceContext = TreeMap<String, Any>().apply { put("correlationId", "corr-123") },
            data = TreeMap<String, Any>().apply { put("testKey", "testValue") }
        )
        
        val mergeEngine = ContextMergeEngine()
        val snapshot = mergeEngine.captureContextSnapshot(context)
        
        assertNotNull(snapshot)
        assertEquals(5, snapshot.contextVersion)
        assertEquals("corr-123", snapshot.traceContext?.get("correlationId"))
        assertEquals("testValue", snapshot.data?.get("testKey"))
        
        logger.info("Context snapshot test PASSED")
    }
    
    companion object {
        fun cleanUp(testEnv: TestWorkflowEnvironment?) {
            testEnv?.close()
        }
    }
}
