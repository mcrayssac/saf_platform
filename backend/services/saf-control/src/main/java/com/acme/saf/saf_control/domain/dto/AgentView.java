package com.acme.saf.saf_control.domain.dto;

public record AgentView(
        String id,
        String type,
        String status,   // starting|running|stopped (mock)
        String node,      // e.g. runtime-mock-1
        int port,
        String host
) {}
