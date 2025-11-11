package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.AgentStatus;
import com.acme.saf.saf_control.domain.dto.AgentStatistics;
import com.acme.saf.saf_control.domain.dto.AgentView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AgentMonitoringService {
    
    private final ControlService controlService;
    private final Map<String, Instant> heartbeats = new ConcurrentHashMap<>();
    private final Duration heartbeatTimeout = Duration.ofSeconds(60);

    public AgentMonitoringService(ControlService controlService) {
        this.controlService = controlService;
    }

    /**
     * Enregistre un heartbeat pour un agent
     */
    public void recordHeartbeat(String agentId) {
        heartbeats.put(agentId, Instant.now());
    }

    /**
     * Récupère tous les agents avec leur statut à jour
     */
    public Collection<AgentView> getAllAgentsWithStatus() {
        return controlService.list().stream()
            .map(this::enrichWithStatus)
            .collect(Collectors.toList());
    }

    /**
     * Récupère uniquement les agents actifs
     */
    public Collection<AgentView> getActiveAgents() {
        return getAllAgentsWithStatus().stream()
            .filter(a -> a.status() == AgentStatus.ACTIVE)
            .collect(Collectors.toList());
    }

    /**
     * Récupère les agents par type
     */
    public Collection<AgentView> getAgentsByType(String type) {
        return getAllAgentsWithStatus().stream()
            .filter(a -> a.type().equalsIgnoreCase(type))
            .collect(Collectors.toList());
    }

    /**
     * Calcule les statistiques
     */
    public AgentStatistics getStatistics() {
        Collection<AgentView> allAgents = getAllAgentsWithStatus();
        long total = allAgents.size();
        long active = allAgents.stream().filter(a -> a.status() == AgentStatus.ACTIVE).count();
        long inactive = allAgents.stream().filter(a -> a.status() == AgentStatus.INACTIVE).count();
        
        // Compte par type
        Map<String, Long> byType = allAgents.stream()
            .collect(Collectors.groupingBy(AgentView::type, Collectors.counting()));
        
        return new AgentStatistics(total, active, inactive, byType.size());
    }

    /**
     * Vérifie périodiquement la santé des agents (toutes les 30s)
     */
    @Scheduled(fixedRate = 30000)
    public void checkAgentHealth() {
        Instant now = Instant.now();

        // Nettoie les heartbeats des agents qui n'existent plus
        Set<String> existingIds = controlService.list().stream()
            .map(AgentView::id)
            .collect(Collectors.toSet());

        heartbeats.keySet().retainAll(existingIds);

        // Vérifie l'état des agents
        controlService.list().forEach(agent -> {
            if (isAgentStale(agent.id(), now)) {
                System.out.println("⚠️ Agent " + agent.id() + " (" + agent.type() + ") est INACTIF");
                // Appel au service de supervision
                supervisionService.handle(agent);
            }
        });
    }


    /**
     * Enrichit un AgentView avec son statut actuel
     */
    private AgentView enrichWithStatus(AgentView agent) {
        Instant lastHb = heartbeats.getOrDefault(agent.id(), null);
        AgentStatus status = determineStatus(lastHb);
        
        return new AgentView(
            agent.id(),
            agent.type(),
            agent.state(),
            agent.runtimeNode(),
            agent.host(),
            agent.port(),
            status,
            lastHb != null ? lastHb : agent.lastHeartbeat()
        );
    }

    /**
     * Détermine le statut en fonction du dernier heartbeat
     */
    private AgentStatus determineStatus(Instant lastHeartbeat) {
        if (lastHeartbeat == null) {
            return AgentStatus.UNKNOWN;
        }
        
        Duration timeSinceLastHb = Duration.between(lastHeartbeat, Instant.now());
        return timeSinceLastHb.compareTo(heartbeatTimeout) > 0 
            ? AgentStatus.INACTIVE 
            : AgentStatus.ACTIVE;
    }

    /**
     * Vérifie si un agent est "périmé" (n'a pas envoyé de heartbeat)
     */
    private boolean isAgentStale(String agentId, Instant now) {
        Instant lastHb = heartbeats.get(agentId);
        if (lastHb == null) return true;
        
        Duration timeSince = Duration.between(lastHb, now);
        return timeSince.compareTo(heartbeatTimeout) > 0;
    }
}