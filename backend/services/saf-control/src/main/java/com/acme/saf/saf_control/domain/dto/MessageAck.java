package com.acme.saf.saf_control.domain.dto;

public record MessageAck(
        String correlationId,
        String status // "accepted" (mock)
) {}

