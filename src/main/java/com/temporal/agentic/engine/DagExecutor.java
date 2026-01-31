package com.temporal.agentic.engine;

import com.temporal.agentic.models.*;
import java.util.*;

/**
 * DAG execution planner for non-agentic and agentic modes.
 */
public class DagExecutor {

    private final DagTopologyComputer topologyComputer;
    private final ConditionEvaluator conditionEvaluator;
    private final ContextMergeEngine mergeEngine;

    public DagExecutor() {
        this.topologyComputer = new DagTopologyComputer();
        this.conditionEvaluator = new ConditionEvaluator();
        this.mergeEngine = new ContextMergeEngine();
    }

    public DagExecutionPlan planExecution(DagSpec dagSpec, WorkflowContext context) {
        DagExecutionPlan plan = new DagExecutionPlan();
        plan.setDagVersion(dagSpec.getVersion());
        plan.setMaxParallelNodes(dagSpec.getMaxParallelNodes());

        Map<String, Integer> levels = topologyComputer.computeTopologicalLevels(dagSpec);
        Map<String, String> nodeStatuses = new TreeMap<>();

        for (NodeDefinition node : dagSpec.getNodes()) {
            nodeStatuses.put(node.getId(), "PENDING");
        }

        int stage = 0;
        while (hasNonTerminalNodes(nodeStatuses)) {
            Set<String> readyNodes = topologyComputer.getReadyNodes(dagSpec, nodeStatuses, context, conditionEvaluator);

            if (readyNodes.isEmpty()) {
                break;
            }

            List<String> stagedNodes = new ArrayList<>(readyNodes);
            if (stagedNodes.size() > dagSpec.getMaxParallelNodes()) {
                stagedNodes = stagedNodes.subList(0, dagSpec.getMaxParallelNodes());
            }

            DagExecutionPlan.Stage stageInfo = new DagExecutionPlan.Stage();
            stageInfo.setStageNumber(stage);
            stageInfo.setNodeIds(new ArrayList<>(stagedNodes));

            for (String nodeId : stagedNodes) {
                nodeStatuses.put(nodeId, "SCHEDULED");
            }

            plan.getStages().add(stageInfo);
            stage++;
        }

        plan.setTotalNodes(dagSpec.getNodes().size());
        return plan;
    }

    public Map<String, NodeDefinition> buildNodeMap(DagSpec dagSpec) {
        Map<String, NodeDefinition> map = new TreeMap<>();
        for (NodeDefinition node : dagSpec.getNodes()) {
            map.put(node.getId(), node);
        }
        return map;
    }

    public boolean shouldReplan(DagSpec currentSpec, NodeDefinition completedNode) {
        return completedNode.getPolicy() != null && completedNode.getPolicy().isReplanCheckpoint();
    }

    public ContextMergeEngine getMergeEngine() {
        return mergeEngine;
    }

    public DagTopologyComputer getTopologyComputer() {
        return topologyComputer;
    }

    public ConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    private boolean hasNonTerminalNodes(Map<String, String> statuses) {
        for (String status : statuses.values()) {
            if (status.equals("PENDING") || status.equals("SCHEDULED")) {
                return true;
            }
        }
        return false;
    }

    public static class DagExecutionPlan {
        private int dagVersion;
        private int maxParallelNodes;
        private int totalNodes;
        private List<Stage> stages = new ArrayList<>();

        public int getDagVersion() { return dagVersion; }
        public void setDagVersion(int dagVersion) { this.dagVersion = dagVersion; }

        public int getMaxParallelNodes() { return maxParallelNodes; }
        public void setMaxParallelNodes(int maxParallelNodes) { this.maxParallelNodes = maxParallelNodes; }

        public int getTotalNodes() { return totalNodes; }
        public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }

        public List<Stage> getStages() { return stages; }
        public void setStages(List<Stage> stages) { this.stages = stages; }

        public static class Stage {
            private int stageNumber;
            private List<String> nodeIds;

            public int getStageNumber() { return stageNumber; }
            public void setStageNumber(int stageNumber) { this.stageNumber = stageNumber; }

            public List<String> getNodeIds() { return nodeIds; }
            public void setNodeIds(List<String> nodeIds) { this.nodeIds = nodeIds; }
        }
    }
}
