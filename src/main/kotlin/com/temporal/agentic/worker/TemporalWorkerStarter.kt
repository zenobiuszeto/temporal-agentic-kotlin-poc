package com.temporal.agentic.worker

import com.temporal.agentic.activities.TemplateNodeActivitiesImpl
import com.temporal.agentic.workflows.AgenticDagParentWorkflowImpl
import com.temporal.agentic.workflows.NodeChildWorkflowImpl
import com.temporal.agentic.workflows.NonAgenticDagParentWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("TemporalWorkerStarter")

fun main() {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)
    
    val factory = WorkerFactory.newInstance(client)
    
    val worker: Worker = factory.newWorker("comm_template_tasks")
    
    worker.registerWorkflowImplementationTypes(
        NonAgenticDagParentWorkflowImpl::class.java,
        AgenticDagParentWorkflowImpl::class.java,
        NodeChildWorkflowImpl::class.java
    )
    
    worker.registerActivitiesImplementations(TemplateNodeActivitiesImpl())
    
    val defaultWorker: Worker = factory.newWorker("default")
    defaultWorker.registerWorkflowImplementationTypes(
        NonAgenticDagParentWorkflowImpl::class.java,
        AgenticDagParentWorkflowImpl::class.java,
        NodeChildWorkflowImpl::class.java
    )
    defaultWorker.registerActivitiesImplementations(TemplateNodeActivitiesImpl())
    
    factory.start()
    logger.info("Temporal worker started and listening on task queues: comm_template_tasks, default")
}
