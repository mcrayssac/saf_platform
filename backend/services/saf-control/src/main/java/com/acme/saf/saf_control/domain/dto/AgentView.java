package com.acme.saf.saf_control.domain.dto;

import com.acme.saf.saf_control.domain.dto.AgentStatus;
import java.time.Instant;

public record AgentView(
    String id,
    String type,
    String state,
    String runtimeNode,
    String host,              // NOUVEAU
    int port,                 // NOUVEAU
    AgentStatus status,       // NOUVEAU
    Instant lastHeartbeat     // NOUVEAU
) {
    // Constructeur de compatibilité avec votre code existant
    public AgentView(String id, String type, String state, String runtimeNode) {
        this(id, type, state, runtimeNode, "unknown", 0, AgentStatus.UNKNOWN, Instant.now());
    }

    // Constructeur avec localisation réseau
    public AgentView(String id, String type, String state, String runtimeNode, String host, int port) {
        this(id, type, state, runtimeNode, host, port, AgentStatus.ACTIVE, Instant.now());
    }
}