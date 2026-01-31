package com.temporal.agentic.engine;

import com.temporal.agentic.models.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes topological ordering and node readiness in a DAG.
 */
public class DagTopologyComputer {

    public Map<String, Integer> computeTopologicalLevels(DagSpec dagSpec) {
        Map<String, Integer> levels = new TreeMap<>();
        Map<String, Set<String>> incomingEdges = buildIncomingEdgesMap(dagSpec);
        Map<String, Set<String>> outgoingEdges = buildOutgoingEdgesMap(dagSpec);

        for (NodeDefinition node : dagSpec.getNodes()) {
            levels.put(node.getId(), 0);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (NodeDefinition node : dagSpec.getNodes()) {
                Set<String> incoming = incomingEdges.getOrDefault(node.getId(), new HashSet<>());
                int maxLevel = 0;
                for (String inNodeId : incoming) {
                    int inLevel = levels.getOrDefault(inNodeId, 0);
                    if (inLevel >= maxLevel) {
                        maxLevel = inLevel + 1;
                    }
                }
                int currentLevel = levels.get(node.getId());
                if (maxLevel > currentLevel) {
                    levels.put(node.getId(), maxLevel);
                    changed = true;
                }
            }
        }

        return levels;
    }

    public Set<String> getReadyNodes(DagSpec dagSpec, Map<String, String> nodeStatuses,
                                    WorkflowContext context, ConditionEvaluator evaluator) {
        Set<String> ready = new TreeSet<>();
        Map<String, Set<String>> incomingEdges = buildIncomingEdgesMap(dagSpec);

        for (NodeDefinition node : dagSpec.getNodes()) {
            if (nodeStatuses.getOrDefault(node.getId(), "PENDING").equals("PENDING")) {
                Set<String> incoming = incomingEdges.getOrDefault(node.getId(), new HashSet<>());

                boolean allPredecessorsComplete = true;
                for (String predId : incoming) {
                    String predStatus = nodeStatuses.getOrDefault(predId, "PENDING");
                    if (!predStatus.equals("SUCCESS") && !predStatus.equals("SKIPPED")) {
                        allPredecessorsComplete = false;
                        break;
                    }
                }

                if (allPredecessorsComplete) {
                    if (node.getCondition() == null || node.getCondition().trim().isEmpty() ||
                        evaluator.evaluate(node.getCondition(), context.getData())) {
                        ready.add(node.getId());
                    }
                }
            }
        }

        return ready;
    }

    private Map<String, Set<String>> buildIncomingEdgesMap(DagSpec dagSpec) {
        Map<String, Set<String>> map = new TreeMap<>();
        for (EdgeDefinition edge : dagSpec.getEdges()) {
            map.computeIfAbsent(edge.getTo(), k -> new TreeSet<>()).add(edge.getFrom());
        }
        return map;
    }

    private Map<String, Set<String>> buildOutgoingEdgesMap(DagSpec dagSpec) {
        Map<String, Set<String>> map = new TreeMap<>();
        for (EdgeDefinition edge : dagSpec.getEdges()) {
            map.computeIfAbsent(edge.getFrom(), k -> new TreeSet<>()).add(edge.getTo());
        }
        return map;
    }

    public List<String> computeDeterministicNodeOrder(Set<String> readyNodes) {
        return new ArrayList<>(readyNodes).stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
