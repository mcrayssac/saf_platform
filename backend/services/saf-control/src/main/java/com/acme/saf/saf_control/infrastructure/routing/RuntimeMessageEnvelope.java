package com.acme.saf.saf_control.infrastructure.routing;

public record RuntimeMessageEnvelope(
        String agentId,
        String node,          // runtime cible (ex: "runtime-mock-1")
        String mode,          // tell / ask
        Object payload,
        Long timeoutMs,
        String correlationId
) {}
