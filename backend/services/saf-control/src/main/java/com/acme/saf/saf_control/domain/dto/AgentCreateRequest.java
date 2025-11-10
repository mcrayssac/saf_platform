package com.acme.saf.saf_control.domain.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record AgentCreateRequest(
        @NotBlank String type,
        Map<String, Object> params
) {}

