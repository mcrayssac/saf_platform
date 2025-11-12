package com.acme.saf.saf_control.domain.dto;

import com.acme.saf.saf_control.domain.dto.AgentStatus;
import com.acme.saf.saf_control.domain.dto.Agent.SupervisionPolicy;
import java.time.Instant;

public record AgentView(
        String id,
        String type,
        String state,
        String runtimeNode,
        String host,
        int port,
        AgentStatus status,
        Instant lastHeartbeat,
        SupervisionPolicy policy
) {
    // Constructeur de compatibilité
    public AgentView(String id, String type, String state, String runtimeNode) {
        this(id, type, state, runtimeNode, "unknown", 0, AgentStatus.UNKNOWN, Instant.now(), SupervisionPolicy.RESUME);
    }

    // Constructeur avec localisation réseau
    public AgentView(String id, String type, String state, String runtimeNode, String host, int port) {
        this(id, type, state, runtimeNode, host, port, AgentStatus.ACTIVE, Instant.now(), SupervisionPolicy.RESUME);
    }

    // Constructeur complet utilisé pour les mises à jour
    public AgentView(String id, String type, String state, String runtimeNode, String host, int port, AgentStatus status, Instant lastHeartbeat, SupervisionPolicy policy) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.runtimeNode = runtimeNode;
        this.host = host;
        this.port = port;
        this.status = status;
        this.lastHeartbeat = lastHeartbeat;
        this.policy = policy;
    }
}
