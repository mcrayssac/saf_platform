package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.AgentStatus;
import com.acme.saf.saf_control.domain.dto.AgentStatistics;
import com.acme.saf.saf_control.domain.dto.AgentView;
import com.acme.saf.saf_control.domain.dto.Agent.SupervisionPolicy;
import com.acme.saf.saf_control.infrastructure.events.RuntimeEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AgentMonitoringService {
    private final SupervisionService supervisionService;
    private final ControlService controlService;

    // Heartbeat registry
    private final Map<String, Instant> heartbeats = new ConcurrentHashMap<>();

    // Agents considérés comme "périmés" après 60 sec sans heartbeat
    private final Duration heartbeatTimeout = Duration.ofSeconds(60);

    // Set des agents mis en quarantaine (ne doivent PAS être supervisés)
    private final Set<String> quarantine = ConcurrentHashMap.newKeySet();


    public AgentMonitoringService(ControlService controlService, SupervisionService supervisionService) {
        this.controlService = controlService;
        this.supervisionService = supervisionService;
    }

    /**
     * Enregistre un heartbeat pour un agent
     */
    public void recordHeartbeat(String agentId) {
        heartbeats.put(agentId, Instant.now());

        // S’il envoie un heartbeat → sortie de quarantaine automatique
        quarantine.remove(agentId);
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
     * Calcule les statistiques (avec dictionnaire complet)
     */
    public AgentStatistics getStatistics() {
        Collection<AgentView> allAgents = getAllAgentsWithStatus();
        long total = allAgents.size();
        long active = allAgents.stream().filter(a -> a.status() == AgentStatus.ACTIVE).count();
        long inactive = allAgents.stream().filter(a -> a.status() == AgentStatus.INACTIVE).count();

        Map<String, Long> byType = allAgents.stream()
                .collect(Collectors.groupingBy(AgentView::type, Collectors.counting()));

        return new AgentStatistics(total, active, inactive, byType);
    }

    /**
     * Vérifie périodiquement la santé des agents (toutes les 30s)
     */
    @Scheduled(fixedRate = 30000)
    public void checkAgentHealth() {
        Instant now = Instant.now();

        // Nettoie les heartbeats des agents supprimés
        Set<String> existingIds = controlService.list().stream()
                .map(AgentView::id)
                .collect(Collectors.toSet());

        heartbeats.keySet().retainAll(existingIds);

        // Vérifie l’état des agents
        controlService.list().forEach(agent -> {

            // Agents en quarantaine : on ignore totalement la supervision
            if (quarantine.contains(agent.id())) {
                return;
            }

            // Aucun heartbeat ou heartbeat expiré → statut INACTIF
            if (isAgentStale(agent.id(), now)) {
                System.out.println("⚠️ Agent " + agent.id() + " (" + agent.type() + ") est INACTIF");

                // → Appel supervision (SEULEMENT si pas en quarantaine)
                supervisionService.handle(agent);

                // → Mise en quarantaine pour éviter boucle infinie
                quarantine.add(agent.id());
            }
        });
    }

    /**
     * Enrichit un AgentView avec son statut actuel
     */
    private AgentView enrichWithStatus(AgentView agent) {
        Instant lastHb = heartbeats.get(agent.id());
        AgentStatus status = getStatusFor(agent.id());

        return new AgentView(
                agent.id(),
                agent.type(),
                agent.state(),
                agent.runtimeNode(),
                agent.host(),
                agent.port(),
                status,
                lastHb,
                agent.policy()
        );
    }

    /**
     * Détermine le statut selon OPTION B
     */
    public AgentStatus getStatusFor(String agentId) {
        Instant last = heartbeats.get(agentId);

        if (last == null) {
            return AgentStatus.INACTIVE;
        }

        Duration diff = Duration.between(last, Instant.now());
        return diff.compareTo(heartbeatTimeout) < 0
                ? AgentStatus.ACTIVE
                : AgentStatus.INACTIVE;
    }

    /**
     * Récupère le dernier heartbeat
     */
    public Instant getLastHeartbeat(String agentId) {
        return heartbeats.get(agentId);
    }

    /**
     * Vérifie si un agent est périmé
     */
    private boolean isAgentStale(String agentId, Instant now) {
        Instant lastHb = heartbeats.get(agentId);
        if (lastHb == null) return true;

        Duration timeSince = Duration.between(lastHb, now);
        return timeSince.compareTo(heartbeatTimeout) > 0;
    }

    public void processEvent(RuntimeEvent event) {

        switch (event.type()) {
            case "ActorStarted" -> recordHeartbeat(event.agentId());
            case "ActorStopped" -> quarantine.add(event.agentId());
            case "ActorFailed"  -> {
                quarantine.add(event.agentId());
                supervisionService.handle(controlService.get(event.agentId()).orElseThrow());
            }
        }
    }


    /**
     * unique générateur officiel d’AgentView
     */
    public AgentView buildAgentView(String id) {
        var agent = controlService.get(id).orElseThrow();
        AgentStatus status = getStatusFor(id);
        Instant lastHb = getLastHeartbeat(id);

        return new AgentView(
                agent.id(),
                agent.type(),
                agent.state(),
                agent.runtimeNode(),
                agent.host(),
                agent.port(),
                status,
                lastHb,
                SupervisionPolicy.RESUME
        );
    }
}
