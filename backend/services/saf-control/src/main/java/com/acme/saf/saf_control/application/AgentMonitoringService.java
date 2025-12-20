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

    // ========== REGISTRES EN MÉMOIRE ==========

    /**
     * Registre des derniers heartbeats reçus par agent.
     */
    private final Map<String, Instant> heartbeats = new ConcurrentHashMap<>();

    /**
     * Délai après lequel un agent est considéré comme "périmé" (stale).
     * Si aucun heartbeat depuis 60 secondes → statut INACTIVE
     */
    private final Duration heartbeatTimeout = Duration.ofSeconds(60);

    /**
     * Set des agents mis en quarantaine.
     */
    private final Set<String> quarantine = ConcurrentHashMap.newKeySet();


    public AgentMonitoringService(ControlService controlService, SupervisionService supervisionService) {
        this.controlService = controlService;
        this.supervisionService = supervisionService;
    }

    // ========== GESTION DES HEARTBEATS ==========

    /**
     * Enregistre un heartbeat pour un agent.
     *
     * IMPORTANT : On ne sort PAS l'agent de quarantaine automatiquement.
     * La sortie de quarantaine doit être une décision manuelle de l'opérateur
     * pour éviter qu'un agent défectueux se relance en boucle.
     *
     * @param agentId Identifiant de l'agent qui envoie le heartbeat
     */
    public void recordHeartbeat(String agentId) {
        heartbeats.put(agentId, Instant.now());
    }

    /**
     * Sort manuellement un agent de quarantaine.
     * À utiliser après avoir vérifié que l'agent est stable.
     *
     * @param agentId Identifiant de l'agent à libérer
     */
    public void releaseFromQuarantine(String agentId) {
        boolean removed = quarantine.remove(agentId);
        if (removed) {
            System.out.println("Agent " + agentId + " sorti de quarantaine manuellement");
        }
    }

    /**
     * Vérifie si un agent est actuellement en quarantaine.
     */
    public boolean isInQuarantine(String agentId) {
        return quarantine.contains(agentId);
    }

    // ========== CONSULTATION DES AGENTS ==========

    /**
     * Récupère tous les agents avec leur statut à jour (ACTIVE/INACTIVE).
     */
    public Collection<AgentView> getAllAgentsWithStatus() {
        return controlService.list().stream()
                .map(this::enrichWithStatus)
                .collect(Collectors.toList());
    }

    /**
     * Récupère uniquement les agents actifs (qui répondent aux heartbeats).
     */
    public Collection<AgentView> getActiveAgents() {
        return getAllAgentsWithStatus().stream()
                .filter(a -> a.status() == AgentStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les agents par type (CLIENT, VILLE, CAPTEUR).
     */
    public Collection<AgentView> getAgentsByType(String type) {
        return getAllAgentsWithStatus().stream()
                .filter(a -> a.type().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    /**
     * Calcule les statistiques globales sur tous les agents.
     * Retourne : nombre total, actifs, inactifs, et répartition par type.
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

    // ========== VÉRIFICATION PÉRIODIQUE DE SANTÉ ==========

    /**
     * Vérifie périodiquement la santé des agents (toutes les 30 secondes).
     */
    @Scheduled(fixedRate = 30000)
    public void checkAgentHealth() {
        Instant now = Instant.now();

        // 1. Nettoie les heartbeats des agents qui n'existent plus
        Set<String> existingIds = controlService.list().stream()
                .map(AgentView::id)
                .collect(Collectors.toSet());
        heartbeats.keySet().retainAll(existingIds);

        // 2. Vérifie l'état de chaque agent
        controlService.list().forEach(agent -> {

            // RÈGLE IMPORTANTE : On ignore les agents en quarantaine
            // Ils ont déjà été traités et ne doivent plus déclencher de supervision
            if (quarantine.contains(agent.id())) {
                return;  // Passe à l'agent suivant
            }

            // Si l'agent est périmé (pas de heartbeat récent)
            if (isAgentStale(agent.id(), now)) {
                System.out.println("Agent " + agent.id() + " (" + agent.type() + ") est INACTIF");

                // 1. D'abord on applique la politique de supervision
                supervisionService.handle(agent);

                // 2. PUIS on met en quarantaine pour éviter la boucle infinie
                System.out.println("Agent " + agent.id() + " mis en quarantaine");
            }
        });
    }

    // ========== GESTION DES ÉVÉNEMENTS RUNTIME ==========

    /**
     * Traite les événements remontés par le Runtime (moteur d'exécution des agents).
     * @param event Événement provenant du Runtime
     */
    public void processEvent(RuntimeEvent event) {
        switch (event.type()) {
            case "ActorStarted" -> {
                // Agent démarré : on enregistre un premier heartbeat
                recordHeartbeat(event.agentId());
                System.out.println("Agent " + event.agentId() + " démarré");
            }

            case "ActorStopped" -> {
                // Agent arrêté proprement : on le met en quarantaine
                // car un agent arrêté ne devrait pas redémarrer automatiquement
                quarantine.add(event.agentId());
                System.out.println("Agent " + event.agentId() + " arrêté et mis en quarantaine");
            }

            case "ActorFailed" -> {
                // CORRECTION : Ordre d'exécution modifié
                // 1. D'abord on applique la politique de supervision
                supervisionService.handle(
                        controlService.get(event.agentId())
                                .orElseThrow(() -> new IllegalStateException("Agent " + event.agentId() + " introuvable"))
                );

                // 2. PUIS on met en quarantaine pour éviter la boucle infinie
                quarantine.add(event.agentId());
                System.out.println("Agent " + event.agentId() + " en échec → supervisé puis mis en quarantaine");
            }
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Enrichit un AgentView avec son statut actuel (ACTIVE/INACTIVE).
     * Unique générateur officiel d'AgentView avec statut à jour.
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
     * Détermine le statut d'un agent selon son dernier heartbeat.
     *
     * LOGIQUE :
     * - Pas de heartbeat enregistré → INACTIVE
     * - Heartbeat < 60 sec → ACTIVE
     * - Heartbeat >= 60 sec → INACTIVE
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
     * Récupère le dernier heartbeat enregistré pour un agent.
     */
    public Instant getLastHeartbeat(String agentId) {
        return heartbeats.get(agentId);
    }

    /**
     * Vérifie si un agent est périmé (stale).
     * Un agent est périmé s'il n'a pas envoyé de heartbeat depuis plus de 60 secondes.
     */
    private boolean isAgentStale(String agentId, Instant now) {
        Instant lastHb = heartbeats.get(agentId);
        if (lastHb == null) return true;

        Duration timeSince = Duration.between(lastHb, now);
        return timeSince.compareTo(heartbeatTimeout) > 0;
    }

    /**
     * Construit un AgentView complet avec statut et heartbeat à jour.
     * Unique générateur officiel d'AgentView.
     */
    public AgentView buildAgentView(String id) {
        var agent = controlService.get(id)
                .orElseThrow(() -> new IllegalStateException("Agent " + id + " introuvable"));

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
                agent.policy()
        );
    }
}