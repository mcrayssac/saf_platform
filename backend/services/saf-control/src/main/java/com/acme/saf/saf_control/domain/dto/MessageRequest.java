package com.acme.saf.saf_control.domain.dto;

public record MessageRequest(
        String mode,            // "tell" | "ask" (mocked)
        Object payload,
        Long timeoutMs
) {}
