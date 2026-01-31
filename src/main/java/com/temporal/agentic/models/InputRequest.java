package com.temporal.agentic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * Request input to the parent workflow.
 */
public class InputRequest implements Serializable {
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("workflowId")
    private String workflowId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("channel")
    private String channel; // e.g., "EMAIL", "SMS"

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("params")
    private Map<String, Object> params;

    public InputRequest() {}

    public InputRequest(String requestId, String workflowId, String eventType,
                        String channel, String locale, Map<String, Object> data,
                        Map<String, Object> params) {
        this.requestId = requestId;
        this.workflowId = workflowId;
        this.eventType = eventType;
        this.channel = channel;
        this.locale = locale;
        this.data = data;
        this.params = params;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
