package com.acme.saf.saf_control.domain.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Agent {
    private final String agentId;
    private final AgentType type;
    private final String host;
    private final int port;
    private AgentStatus status;
    private Instant lastHeartbeat;
    private final Map<String, String> metadata;
    private SupervisionPolicy policy;

    public enum SupervisionPolicy {
        RESTART,
        RESUME,
        STOP,
        QUARANTINE
    }

    public Agent(String agentId, AgentType type, String host, int port) {
        this.agentId = agentId;
        this.type = type;
        this.host = host;
        this.port = port;
        this.status = AgentStatus.ACTIVE;
        this.lastHeartbeat = Instant.now();
        this.metadata = new HashMap<>();
        this.policy = SupervisionPolicy.RESTART;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = Instant.now();
        this.status = AgentStatus.ACTIVE;
    }

    public void markAsInactive() {
        this.status = AgentStatus.INACTIVE;
    }

    public boolean isStale(Duration threshold) {
        return Duration.between(lastHeartbeat, Instant.now())
            .compareTo(threshold) > 0;
    }

    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    // Getters
    public String getAgentId() { return agentId; }
    public AgentType getType() { return type; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public AgentStatus getStatus() { return status; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    public SupervisionPolicy getPolicy() { return policy; }
    public void setPolicy(SupervisionPolicy policy) { this.policy = policy; }
}