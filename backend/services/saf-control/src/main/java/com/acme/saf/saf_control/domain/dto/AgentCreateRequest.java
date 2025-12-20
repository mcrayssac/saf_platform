package com.acme.saf.saf_control.domain.dto;

import com.acme.saf.saf_control.domain.dto.Agent.SupervisionPolicy;

/**
 * Requête pour créer un nouvel agent.
 */
public record AgentCreateRequest(
        String type,
        String host,
        int port,
        SupervisionPolicy policy  // politique de supervision configurable
) {
    public AgentCreateRequest(String type) {
        this(type, "localhost", 8080, SupervisionPolicy.RESTART);
    }

    /**
     * Constructeur avec localisation réseau mais policy par défaut.
     */
    public AgentCreateRequest(String type, String host, int port) {
        this(type, host, port, SupervisionPolicy.RESTART);
    }
}