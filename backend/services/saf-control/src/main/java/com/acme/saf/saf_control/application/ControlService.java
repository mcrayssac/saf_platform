package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.*;
import com.acme.saf.saf_control.infrastructure.events.EventBus;
import com.acme.saf.saf_control.infrastructure.routing.RuntimeGateway;
import com.acme.saf.saf_control.infrastructure.routing.RuntimeMessageEnvelope;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ControlService {

    private final Map<String, AgentView> registry = new ConcurrentHashMap<>();
    private final EventBus events;
    private final RuntimeGateway runtimeGateway;

    public ControlService(EventBus events, RuntimeGateway runtimeGateway) {
        this.events = events;
        this.runtimeGateway = runtimeGateway;
    }

    // MOCK registry pour les agents
    public Collection<AgentView> list() {
        return registry.values();
    }

    public AgentView get(String id) {
        return registry.get(id);  // Retourne null si l'agent n'existe pas
    }

    public AgentView create(AgentCreateRequest req) {
        String id = UUID.randomUUID().toString();        
        String host = req.host() != null ? req.host() : "localhost";
        int port = req.port() != 0 ? req.port() : 8080;        

        AgentView view = new AgentView(id, req.type(), "starting", "runtime-mock-1", port, host);
        registry.put(id, view);
        events.publish("ActorStarted", view);
        // simulate that it's quickly "running"
        AgentView running = new AgentView(id, req.type(), "running", "runtime-mock-1", port, host);
        registry.put(id, running);
        events.publish("ActorRunning", running);
        return running;
    }

    public void destroy(String id) {
        AgentView removed = registry.remove(id);
        if (removed != null) {
            events.publish("AgentDestroyed", Map.of("id", id));
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
