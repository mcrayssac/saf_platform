package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.*;
import com.acme.saf.saf_control.infrastructure.events.EventBus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ControlService {
    private final Map<String, AgentView> registry = new ConcurrentHashMap<>();
    private final EventBus events;

    public ControlService(EventBus events) { this.events = events; }

    public Collection<AgentView> list() { return registry.values(); }

    public Optional<AgentView> get(String id) { return Optional.ofNullable(registry.get(id)); }

    public AgentView spawn(AgentCreateRequest req) {
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
        if (removed != null) events.publish("ActorStopped", removed);
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
