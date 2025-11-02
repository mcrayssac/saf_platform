package com.acme.saf.saf_control.domain.dto;

public record AgentStatistics(
    long totalAgents,
    long activeAgents,
    long inactiveAgents,
    long agentsByType
) {}