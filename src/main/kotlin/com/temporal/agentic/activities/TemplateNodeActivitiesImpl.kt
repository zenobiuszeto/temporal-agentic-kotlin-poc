package com.temporal.agentic.activities

import com.temporal.agentic.models.*
import io.temporal.activity.Activity
import java.util.*

class TemplateNodeActivitiesImpl : TemplateNodeActivities {
    
    override fun executeValidate(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        
        val errors = mutableListOf<String>()
        val outputs = TreeMap<String, Any>()
        
        val eventType = input["eventType"] as? String
        val channel = input["channel"] as? String
        val locale = input["locale"] as? String
        
        if (eventType.isNullOrBlank()) {
            errors.add("eventType is required")
        }
        if (channel == null || channel !in listOf("EMAIL", "SMS", "PUSH")) {
            errors.add("channel must be EMAIL, SMS, or PUSH")
        }
        if (locale.isNullOrBlank()) {
            errors.add("locale is required")
        }
        
        outputs["isValid"] = errors.isEmpty()
        outputs["validationErrors"] = errors
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = TreeMap(input),
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("VALIDATE completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = if (errors.isEmpty()) "SUCCESS" else "FAILED",
            outputs = outputs,
            contextDelta = delta,
            errors = errors,
            metrics = TreeMap()
        )
    }
    
    override fun executeGovernance(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        
        val disclaimers = mutableListOf<String>()
        val outputs = TreeMap<String, Any>()
        
        val eventType = input["eventType"] as? String
        
        when {
            "FRAUD_ALERT".equals(eventType, ignoreCase = true) -> {
                disclaimers.add("Reg E Dispute rights notification required")
                disclaimers.add("Consumer account protection disclosure")
            }
            "STATEMENT_READY".equals(eventType, ignoreCase = true) -> {
                disclaimers.add("Standard statement availability notice")
            }
        }
        
        outputs["complianceApproved"] = true
        outputs["disclaimers"] = disclaimers
        
        val writes = TreeMap<String, Any>()
        writes["disclaimers"] = disclaimers
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = writes,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("GOVERNANCE completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executeRetrieve(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        
        val templates = TreeMap<String, Any>()
        templates["STATEMENT_READY"] = "Your statement for account ending in XXXX is ready."
        templates["FRAUD_ALERT"] = "We detected suspicious activity on your account."
        templates["ACH_RETURN"] = "An ACH transfer was returned: {{reason}}"
        
        val brandRules = TreeMap<String, Any>()
        brandRules["tone"] = "professional"
        brandRules["maxChars"] = 160
        
        val outputs = TreeMap<String, Any>()
        outputs["templates"] = templates
        outputs["brandRules"] = brandRules
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = outputs,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("RETRIEVE completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executeTemplateMap(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        val contextSnapshot = request.contextSnapshot
        
        val eventType = input["eventType"] as? String
        
        val filledContent = when {
            "FRAUD_ALERT".equals(eventType, ignoreCase = true) ->
                "We detected suspicious activity on your account. " +
                "Your recent transaction of \$500 at XYZ Store has been flagged."
            else ->
                "Your statement for account ending in 5678 is ready. " +
                "Please log in to review details."
        }
        
        val selectedTemplate = TreeMap<String, Any>()
        selectedTemplate["templateId"] = eventType ?: ""
        selectedTemplate["content"] = filledContent
        
        val outputs = TreeMap<String, Any>()
        outputs["selectedTemplate"] = selectedTemplate
        outputs["filledContent"] = filledContent
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = outputs,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("TEMPLATE_MAP completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executeTransform(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        
        val filledContent = input["filledContent"] as? String ?: ""
        
        val shortVariant = filledContent.truncate(100)
        val longVariant = filledContent
        val smsLimited = filledContent.truncate(160)
        
        val outputs = TreeMap<String, Any>()
        outputs["shortVariant"] = shortVariant
        outputs["longVariant"] = longVariant
        outputs["smsLimited"] = smsLimited
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = outputs,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("TRANSFORM completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executeDecide(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        
        val channel = input["channel"] as? String
        val (selectedVariant, selectionReason) = when {
            "SMS".equals(channel, ignoreCase = true) ->
                (input["smsLimited"] as? String ?: "") to "SMS requires limited characters"
            "EMAIL".equals(channel, ignoreCase = true) ->
                (input["longVariant"] as? String ?: "") to "Email supports full length content"
            else ->
                (input["shortVariant"] as? String ?: "") to "Default to short variant"
        }
        
        val outputs = TreeMap<String, Any>()
        outputs["selectedVariant"] = selectedVariant
        outputs["selectionReason"] = selectionReason
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = outputs,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("DECIDE completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executeAction(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        val idempotencyKey = request.idempotencyKey
        
        val messageId = "MSG-${System.currentTimeMillis()}-$nodeId"
        
        val outputs = TreeMap<String, Any>()
        outputs["messageId"] = messageId
        outputs["sendStatus"] = "SENT"
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = outputs,
            promoteKeys = listOf("messageId"),
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("ACTION completed: $messageId")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executeFinalize(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        val contextSnapshot = request.contextSnapshot
        
        val messageId = input["messageId"] as? String
        val content = input["content"] as? String
        
        val auditTrail = TreeMap<String, Any>()
        auditTrail["messageId"] = messageId ?: ""
        auditTrail["timestamp"] = System.currentTimeMillis()
        auditTrail["contentLength"] = content?.length ?: 0
        auditTrail["contextVersion"] = contextSnapshot?.contextVersion ?: 0
        
        val finalResponse = TreeMap<String, Any>()
        finalResponse["messageId"] = messageId ?: ""
        finalResponse["status"] = "COMPLETED"
        
        val outputs = TreeMap<String, Any>()
        outputs["auditTrail"] = auditTrail
        outputs["finalResponse"] = finalResponse
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = outputs,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("FINALIZE completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    override fun executePlanner(request: NodeExecutionRequest): NodeExecutionResponse {
        val nodeId = request.nodeId!!
        val input = request.nodeInput ?: emptyMap()
        
        val channel = input["channel"] as? String
        
        val plan = TreeMap<String, Any>()
        plan["version"] = 1
        plan["planReason"] = "Initial planning based on channel: $channel"
        
        if ("SMS".equals(channel, ignoreCase = true)) {
            plan["addTransformSmsLimitNode"] = true
        }
        
        val outputs = TreeMap<String, Any>()
        outputs["plan"] = plan
        
        val delta = ContextDelta(
            nodeId = nodeId,
            writes = plan,
            timestamp = System.currentTimeMillis()
        )
        
        Activity.getExecutionContext().heartbeat("PLANNER completed")
        
        return NodeExecutionResponse(
            nodeId = nodeId,
            status = "SUCCESS",
            outputs = outputs,
            contextDelta = delta,
            errors = emptyList(),
            metrics = TreeMap()
        )
    }
    
    private fun String.truncate(maxLength: Int): String {
        return if (length <= maxLength) this else substring(0, maxLength - 3) + "..."
    }
}
