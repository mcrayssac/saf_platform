package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.AgentStatus;
import com.acme.saf.saf_control.domain.dto.*;
import com.acme.saf.saf_control.infrastructure.events.EventBus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public ControlService(EventBus events) { this.events = events; }

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
     * Envoie un message à un agent.
     * @param id ID de l'agent destinataire
     * @param msg Requête de message (mode, payload, timeout)
     * @return Accusé de réception avec ID de corrélation
     */
    public MessageAck sendMessage(String id, MessageRequest msg) {
        String corr = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "id", id,
                "mode", msg.mode(),
                "payload", msg.payload(),
                "timeoutMs", msg.timeoutMs(),
                "correlationId", corr
        );
        events.publish("MessageDelivered", event);
        return new MessageAck(corr, "accepted");
    }
}