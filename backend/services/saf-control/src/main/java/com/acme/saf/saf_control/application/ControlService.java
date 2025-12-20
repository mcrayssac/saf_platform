package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.AgentStatus;
import com.acme.saf.saf_control.domain.dto.*;
import com.acme.saf.saf_control.infrastructure.events.EventBus;
import com.acme.saf.saf_control.infrastructure.routing.RuntimeGateway;
import com.acme.saf.saf_control.infrastructure.routing.RuntimeMessageEnvelope;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ControlService {
    /**
     * Registre central de tous les agents.
     * Clé = ID de l'agent, Valeur = AgentView (vue sur l'agent)
     */
    private final Map<String, AgentView> registry = new ConcurrentHashMap<>();

    /**
     * Bus d'événements pour notifier les autres services des changements.
     */
    private final EventBus events;
    private final RuntimeGateway runtimeGateway;

    public ControlService(EventBus events, RuntimeGateway runtimeGateway) {
        this.events = events;
        this.runtimeGateway = runtimeGateway;
    }

    /**
     * Liste tous les agents enregistrés.
     */
    public Collection<AgentView> list() { return registry.values(); }

    /**
     * Récupère un agent par son ID.
     */
    public Optional<AgentView> get(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    /**
     * Crée et lance un nouvel agent.
     * @param req Requête de création avec type, localisation et politique
     * @return L'agent créé en état "running"
     */
    public AgentView spawn(AgentCreateRequest req) {
        String id = UUID.randomUUID().toString();

        // Extraction des paramètres avec valeurs par défaut
        String host = req.host() != null ? req.host() : "localhost";
        int port = req.port() != 0 ? req.port() : 8080;

        // On utilise la policy de la requête
        var policy = req.policy() != null ? req.policy() : Agent.SupervisionPolicy.RESTART;

        // Phase 1 : Agent en démarrage
        AgentView starting = new AgentView(
                id,
                req.type(),
                "starting",
                "runtime-mock-1",
                host,
                port,
                AgentStatus.INACTIVE,
                null,
                policy
        );
        registry.put(id, starting);
        events.publish("ActorStarted", starting);

        // Phase 2 : Simulation de la transition vers "running"
        // (En production, cet événement viendrait du Runtime)
        AgentView running = new AgentView(
                id,
                req.type(),
                "running",
                "runtime-mock-1",
                host,
                port,
                AgentStatus.ACTIVE,
                Instant.now(),
                policy
        );
        registry.put(id, running);
        events.publish("ActorRunning", running);

        return running;
    }

    /**
     * Met à jour un agent dans le registre.
     * Utilisé notamment pour mettre à jour le statut ou le heartbeat.
     */
    public void update(AgentView updated) {
        registry.put(updated.id(), updated);
    }

    /**
     * Détruit un agent (arrêt et suppression du registre).
     */
    public void destroy(String id) {
        AgentView removed = registry.remove(id);
        if (removed != null) {
            events.publish("ActorStopped", removed);
        }
    }

    /**
     * Get all agents
     */
    public Collection<AgentView> getAllAgents() {
        return registry.values();
    }

    public Collection<AgentView> getAgentsByHost(String host) {
        if (host == null || host.isBlank()) {
            return Collections.emptyList();
        }
        return registry.values().stream()
                .filter(agent -> host.equalsIgnoreCase(agent.host()))
                .collect(Collectors.toList());
    }
    
    public Collection<AgentView> getAgentsByStatus(String status) {
        if (status == null || status.isBlank()) {
            return Collections.emptyList();
        }

        return registry.values().stream()
                .filter(agent -> agent.status().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    /**
     * Envoie un message à un agent.
     * @param id ID de l'agent destinataire
     * @param msg Requête de message (mode, payload, timeout)
     * @return Accusé de réception avec ID de corrélation
     */
    public MessageAck sendMessage(String id, MessageRequest msg) {
        // 1) Vérifier que l’agent existe
        AgentView agent = registry.get(id);
        if (agent == null) {
            throw new NoSuchElementException("Unknown agent " + id);
        }

        // 2) Générer un correlationId
        String corr = UUID.randomUUID().toString();

        // 3) Construire l’enveloppe pour le Runtime
        RuntimeMessageEnvelope envelope = new RuntimeMessageEnvelope(
                id,
                agent.node(),        // Route vers le bon runtime
                msg.mode(),
                msg.payload(),
                msg.timeoutMs(),
                corr
        );

        // 4) Déléguer au gateway (mock aujourd’hui, vrai broker plus tard)
        runtimeGateway.dispatch(envelope);

        // 5) Optionnel : notifier la UI via EventBus comme avant
        Map<String, Object> event = Map.of(
                "id", id,
                "mode", msg.mode(),
                "payload", msg.payload(),
                "timeoutMs", msg.timeoutMs(),
                "correlationId", corr,
                "node", agent.node()
        );
        events.publish("MessageDelivered", event);

        // 6) Retourner l’ACK
        return new MessageAck(corr, "accepted");
    }
    
    @PostConstruct
    public void debugRegistry() {
        // Ajouter un agent de test si le registry est vide
        if (registry.isEmpty()) {
            String testId = UUID.randomUUID().toString();
            registry.put(testId, new AgentView(testId, "test", "running", "runtime-mock-1"));
        }

        System.out.println("Agents in registry:");
        registry.forEach((id, agent) -> System.out.println("ID: " + id + ", Agent: " + agent));
    }
}
