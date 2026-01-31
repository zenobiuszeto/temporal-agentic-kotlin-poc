package com.temporal.agentic.activities;

import com.temporal.agentic.models.*;
import io.temporal.activity.Activity;
import java.util.*;

/**
 * Implementations of template mapping node activities.
 */
public class TemplateNodeActivitiesImpl implements TemplateNodeActivities {

    @Override
    public NodeExecutionResponse executeValidate(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();

        List<String> errors = new ArrayList<>();
        Map<String, Object> outputs = new TreeMap<>();

        String eventType = (String) input.get("eventType");
        String channel = (String) input.get("channel");
        String locale = (String) input.get("locale");

        if (eventType == null || eventType.trim().isEmpty()) {
            errors.add("eventType is required");
        }
        if (channel == null || !List.of("EMAIL", "SMS", "PUSH").contains(channel)) {
            errors.add("channel must be EMAIL, SMS, or PUSH");
        }
        if (locale == null || locale.trim().isEmpty()) {
            errors.add("locale is required");
        }

        outputs.put("isValid", errors.isEmpty());
        outputs.put("validationErrors", errors);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(new TreeMap<>(input));
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus(errors.isEmpty() ? "SUCCESS" : "FAILED");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(errors);
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("VALIDATE completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executeGovernance(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();

        List<String> disclaimers = new ArrayList<>();
        Map<String, Object> outputs = new TreeMap<>();

        String eventType = (String) input.get("eventType");

        if ("FRAUD_ALERT".equalsIgnoreCase(eventType)) {
            disclaimers.add("Reg E Dispute rights notification required");
            disclaimers.add("Consumer account protection disclosure");
        } else if ("STATEMENT_READY".equalsIgnoreCase(eventType)) {
            disclaimers.add("Standard statement availability notice");
        }

        outputs.put("complianceApproved", true);
        outputs.put("disclaimers", disclaimers);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        Map<String, Object> writes = new TreeMap<>();
        writes.put("disclaimers", disclaimers);
        delta.setWrites(writes);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("GOVERNANCE completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executeRetrieve(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();

        Map<String, Object> templates = new TreeMap<>();
        templates.put("STATEMENT_READY", "Your statement for account ending in XXXX is ready.");
        templates.put("FRAUD_ALERT", "We detected suspicious activity on your account.");
        templates.put("ACH_RETURN", "An ACH transfer was returned: {{reason}}");

        Map<String, Object> brandRules = new TreeMap<>();
        brandRules.put("tone", "professional");
        brandRules.put("maxChars", 160);

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("templates", templates);
        outputs.put("brandRules", brandRules);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(outputs);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("RETRIEVE completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executeTemplateMap(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();
        ContextSnapshot contextSnapshot = request.getContextSnapshot();

        String eventType = (String) input.get("eventType");
        Map<String, Object> contextData = contextSnapshot.getData();

        String filledContent;
        if ("FRAUD_ALERT".equalsIgnoreCase(eventType)) {
            filledContent = "We detected suspicious activity on your account. " +
                    "Your recent transaction of $500 at XYZ Store has been flagged.";
        } else {
            filledContent = "Your statement for account ending in 5678 is ready. " +
                    "Please log in to review details.";
        }

        Map<String, Object> selectedTemplate = new TreeMap<>();
        selectedTemplate.put("templateId", eventType);
        selectedTemplate.put("content", filledContent);

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("selectedTemplate", selectedTemplate);
        outputs.put("filledContent", filledContent);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(outputs);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("TEMPLATE_MAP completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executeTransform(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();

        String filledContent = (String) input.get("filledContent");
        String channel = (String) input.get("channel");

        String shortVariant = truncateString(filledContent, 100);
        String longVariant = filledContent;
        String smsLimited = truncateString(filledContent, 160);

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("shortVariant", shortVariant);
        outputs.put("longVariant", longVariant);
        outputs.put("smsLimited", smsLimited);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(outputs);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("TRANSFORM completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executeDecide(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();

        String channel = (String) input.get("channel");
        String selectedVariant;
        String selectionReason;

        if ("SMS".equalsIgnoreCase(channel)) {
            selectedVariant = (String) input.get("smsLimited");
            selectionReason = "SMS requires limited characters";
        } else if ("EMAIL".equalsIgnoreCase(channel)) {
            selectedVariant = (String) input.get("longVariant");
            selectionReason = "Email supports full length content";
        } else {
            selectedVariant = (String) input.get("shortVariant");
            selectionReason = "Default to short variant";
        }

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("selectedVariant", selectedVariant);
        outputs.put("selectionReason", selectionReason);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(outputs);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("DECIDE completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executeAction(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();
        String idempotencyKey = request.getIdempotencyKey();

        String content = (String) input.get("content");
        String channel = (String) input.get("channel");

        // In real scenario, idempotencyKey would be used for DB lookup to prevent duplicates
        String messageId = "MSG-" + System.currentTimeMillis() + "-" + nodeId;

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("messageId", messageId);
        outputs.put("sendStatus", "SENT");

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(outputs);
        delta.setPromoteKeys(List.of("messageId")); // Promote messageId to top-level
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("ACTION completed: " + messageId);
        return response;
    }

    @Override
    public NodeExecutionResponse executeFinalize(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();
        ContextSnapshot contextSnapshot = request.getContextSnapshot();

        String messageId = (String) input.get("messageId");
        String content = (String) input.get("content");

        Map<String, Object> auditTrail = new TreeMap<>();
        auditTrail.put("messageId", messageId);
        auditTrail.put("timestamp", System.currentTimeMillis());
        auditTrail.put("contentLength", content != null ? content.length() : 0);
        auditTrail.put("contextVersion", contextSnapshot.getContextVersion());

        Map<String, Object> finalResponse = new TreeMap<>();
        finalResponse.put("messageId", messageId);
        finalResponse.put("status", "COMPLETED");

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("auditTrail", auditTrail);
        outputs.put("finalResponse", finalResponse);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(outputs);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("FINALIZE completed");
        return response;
    }

    @Override
    public NodeExecutionResponse executePlanner(NodeExecutionRequest request) {
        String nodeId = request.getNodeId();
        Map<String, Object> input = request.getNodeInput();

        // Mock planner logic: check if SMS channel should trigger transform-sms-limit node
        String channel = (String) input.get("channel");

        Map<String, Object> plan = new TreeMap<>();
        plan.put("version", 1);
        plan.put("planReason", "Initial planning based on channel: " + channel);

        if ("SMS".equalsIgnoreCase(channel)) {
            plan.put("addTransformSmsLimitNode", true);
        }

        Map<String, Object> outputs = new TreeMap<>();
        outputs.put("plan", plan);

        ContextDelta delta = new ContextDelta();
        delta.setNodeId(nodeId);
        delta.setWrites(plan);
        delta.setTimestamp(System.currentTimeMillis());

        NodeExecutionResponse response = new NodeExecutionResponse();
        response.setNodeId(nodeId);
        response.setStatus("SUCCESS");
        response.setOutputs(outputs);
        response.setContextDelta(delta);
        response.setErrors(new ArrayList<>());
        response.setMetrics(new TreeMap<>());

        Activity.getExecutionContext().heartbeat("PLANNER completed");
        return response;
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
