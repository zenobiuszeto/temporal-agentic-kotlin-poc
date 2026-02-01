# Temporal Agentic Kotlin POC

A complete Kotlin implementation of an n8n-style DAG orchestrator built on Temporal, featuring both **NON-AGENTIC** (static DAG) and **AGENTIC** (dynamic planning/replanning) orchestration patterns.

## Overview

This project implements a production-ready workflow orchestration engine that:
- Executes directed acyclic graphs (DAGs) of nodes as child workflows
- Maintains versioned workflow context with deterministic merging
- Supports both static DAG execution and AI-driven dynamic replanning
- Provides comprehensive observability, error handling, and compensation patterns

## Architecture

### Core Components

1. **Parent Workflows**
   - `NonAgenticDagParentWorkflow`: Executes a predefined DAG spec deterministically
   - `AgenticDagParentWorkflow`: Includes a Planner node that can modify the DAG at runtime

2. **Child Workflows**
   - `NodeChildWorkflow`: Wraps activities for each node type, ensuring all node execution happens via child workflows

3. **Activities**
   - `TemplateNodeActivities`: Implements all node types (VALIDATE, GOVERNANCE, RETRIEVE, TEMPLATE_MAP, TRANSFORM, DECIDE, ACTION, FINALIZE, PLANNER)

4. **Engine Components**
   - `DagExecutor`: Orchestrates DAG execution planning
   - `DagTopologyComputer`: Computes topological levels and determines ready nodes
   - `ContextMergeEngine`: Handles deterministic context merging
   - `ConditionEvaluator`: Evaluates conditional expressions for branching

### Node Types

- **VALIDATE**: Input validation
- **GOVERNANCE**: Compliance checks and policy enforcement
- **RETRIEVE**: Data retrieval from external sources
- **TEMPLATE_MAP**: Template selection and placeholder filling
- **TRANSFORM**: Data transformation and formatting
- **DECIDE/REASON**: Decision-making logic
- **ACTION**: External actions (e.g., sending communications)
- **FINALIZE**: Final processing and audit trail creation
- **PLANNER**: (Agentic only) Generates or modifies the DAG spec based on context

## Prerequisites

- **JDK 17** or later
- **Gradle 8.5+**
- **Temporal Server** (for production) or embedded test server (for testing)

## Building the Project

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Create distribution
./gradlew distZip
```

## Running the Worker

The worker registers both parent workflows and child workflows, as well as all activities.

```bash
# Run the worker (requires Temporal server running on localhost:7233)
./gradlew run
```

Or using the distribution:

```bash
./build/distributions/temporal-agentic-kotlin-poc-1.0.0/bin/temporal-agentic-kotlin-poc
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/temporal/agentic/
│   │   ├── activities/          # Activity interfaces and implementations
│   │   ├── engine/              # Core execution engine
│   │   ├── models/              # Data models
│   │   ├── workflows/           # Workflow interfaces and implementations
│   │   └── worker/              # Worker startup
│   └── resources/
│       └── specs/               # JSON DAG specifications
│           ├── non_agentic_comm_template.json
│           └── agentic_comm_template.json
└── test/
    └── kotlin/com/temporal/agentic/  # JUnit tests
```

## DAG Specification Format

DAG specs are defined in JSON and include:

```json
{
  "workflowId": "example_workflow",
  "version": 1,
  "maxParallelNodes": 2,
  "mergeStrategy": "DETERMINISTIC",
  "failureStrategy": "FAIL_FAST",
  "nodes": [
    {
      "id": "validate",
      "type": "VALIDATE",
      "name": "Validate Request",
      "inputs": {
        "eventType": "required",
        "channel": "required",
        "locale": "required"
      },
      "taskQueue": "comm_template_tasks",
      "policy": {
        "allowFailure": false,
        "allowRerun": false,
        "isTerminal": false,
        "replanCheckpoint": false
      },
      "timeouts": {
        "startToCloseSeconds": 60,
        "scheduleToCloseSeconds": 300
      },
      "retryPolicy": {
        "initialIntervalSeconds": 1,
        "backoffCoefficient": 2.0,
        "maximumAttempts": 2
      }
    }
  ],
  "edges": [
    {
      "from": "validate",
      "to": "governance",
      "on": "success"
    }
  ]
}
```

## Use Case: Communication Template Mapping

The included test case demonstrates a banking communication workflow:

1. **VALIDATE**: Validates event type, channel, and locale
2. **GOVERNANCE**: Checks compliance requirements (Reg E/Reg CC)
3. **RETRIEVE**: Loads templates and brand rules
4. **TEMPLATE_MAP**: Selects and fills template placeholders
5. **TRANSFORM**: Creates variants (short/long/SMS-limited)
6. **DECIDE**: Selects the best variant for the channel
7. **ACTION**: Sends the communication (mocked)
8. **FINALIZE**: Creates audit trail

### Running the Test

```bash
./gradlew test --tests "*testNonAgenticWorkflowEndToEnd"
./gradlew test --tests "*testAgenticWorkflowEndToEnd"
```

## Key Features

### 1. Deterministic Context Merging

- Versioned context with incremental updates
- Namespaced writes under `context.nodes.<nodeId>.*`
- Optional key promotion to top-level context
- Deterministic ordering by completion order with nodeId tie-breaking

### 2. Conditional Branching

- Simple expression language: `exists(path)`, `equals(path, value)`
- Boolean operators: `and`, `or`, `not`
- Edge-level conditions for routing

### 3. Agentic Mode

- PLANNER node can generate/modify DAG specs
- Replan checkpoints at configurable nodes
- Completed nodes don't rerun unless `allowRerun=true`

### 4. Reliability

- Per-node retry policies with exponential backoff
- Timeouts at node and workflow levels
- Compensation workflows for rollback
- Idempotency key support

### 5. Observability

- Structured logging with correlation IDs
- Workflow queries: `getStatus()`, `getNodeStatuses()`, `getContextSummary()`
- Progress tracking via workflow state
- Detailed metrics in node responses

## Testing

The test suite includes:
- End-to-end workflow execution (non-agentic and agentic)
- Context propagation and merging
- DAG topology computation
- Conditional branching
- Deterministic ordering verification
- Context snapshot capture

Run all tests:
```bash
./gradlew test
```

Run specific test:
```bash
./gradlew test --tests "CommTemplateWorkflowTest.testNonAgenticWorkflowEndToEnd"
```

## Development

### Adding a New Node Type

1. Add the node type to `TemplateNodeActivities` interface
2. Implement the activity method in `TemplateNodeActivitiesImpl`
3. Add routing in `NodeChildWorkflowImpl.routeNodeExecution()`
4. Create a node definition in your DAG spec JSON

### Customizing the Merge Strategy

The `ContextMergeEngine` applies deltas in deterministic order:
1. Sort by completion order (captured in workflow state)
2. Tie-break by nodeId (lexicographic)
3. Apply writes to namespaced location: `nodes.<nodeId>.*`
4. Promote specified keys to top-level context

## Production Considerations

- **Temporal Server**: Run a production Temporal cluster
- **Task Queues**: Use separate task queues for different workload types
- **Monitoring**: Integrate with Temporal Web UI and metrics exporters
- **Scaling**: Run multiple worker instances for horizontal scaling
- **Versioning**: Use workflow versioning for safe upgrades

## License

This is a proof-of-concept project for demonstration purposes.

## Contributing

This project demonstrates Temporal best practices for:
- Deterministic workflow execution
- Child workflow patterns
- Context management
- Error handling and compensation
- Testing with Temporal test environment