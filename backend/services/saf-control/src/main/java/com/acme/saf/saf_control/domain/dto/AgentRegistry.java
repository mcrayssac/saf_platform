package com.acme.saf.saf_control.domain.dto;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {
    void register(Agent agent);
    void updateHeartbeat(String agentId);
    Optional<Agent> findById(String agentId);
    List<Agent> findAll();
    List<Agent> findAllActive();
    List<Agent> findByType(AgentType type);
    void remove(String agentId);
    void markAsInactive(String agentId);
    long countActiveByType(AgentType type);
}