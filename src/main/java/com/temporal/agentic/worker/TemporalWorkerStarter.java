package com.temporal.agentic.worker;

import com.temporal.agentic.activities.TemplateNodeActivitiesImpl;
import com.temporal.agentic.workflows.NodeChildWorkflowImpl;
import com.temporal.agentic.workflows.NonAgenticDagParentWorkflowImpl;
import com.temporal.agentic.workflows.AgenticDagParentWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker starter for Temporal workflows.
 */
public class TemporalWorkerStarter {
    private static final Logger logger = LoggerFactory.getLogger(TemporalWorkerStarter.class);

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Create worker for comm template tasks
        Worker worker = factory.newWorker("comm_template_tasks");

        // Register workflows
        worker.registerWorkflowImplementationTypes(
                NonAgenticDagParentWorkflowImpl.class,
                AgenticDagParentWorkflowImpl.class,
                NodeChildWorkflowImpl.class
        );

        // Register activities
        worker.registerActivitiesImpl(new TemplateNodeActivitiesImpl());

        // Create default task queue worker
        Worker defaultWorker = factory.newWorker("default");
        defaultWorker.registerWorkflowImplementationTypes(
                NonAgenticDagParentWorkflowImpl.class,
                AgenticDagParentWorkflowImpl.class,
                NodeChildWorkflowImpl.class
        );
        defaultWorker.registerActivitiesImpl(new TemplateNodeActivitiesImpl());

        factory.start();
        logger.info("Temporal worker started and listening on task queues: comm_template_tasks, default");
    }
}
