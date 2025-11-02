package com.acme.saf.saf_control.domain.dto;

public record AgentCreateRequest(
    String type,
    String host,    // NOUVEAU
    int port        // NOUVEAU
) {
    // Constructeur de compatibilité
    public AgentCreateRequest(String type) {
        this(type, "localhost", 8080);
    }
}