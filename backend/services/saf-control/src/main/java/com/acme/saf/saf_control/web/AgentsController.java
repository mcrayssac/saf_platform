package com.acme.saf.saf_control.web;

import com.acme.saf.saf_control.application.AgentMonitoringService;
import com.acme.saf.saf_control.application.ControlService;
import com.acme.saf.saf_control.domain.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;

@RestController
@RequestMapping("/agents")
@Tag(name = "Agents")
public class AgentsController {
    
    private final ControlService service;
    private final AgentMonitoringService monitoringService;

    public AgentsController(
        ControlService service,
        AgentMonitoringService monitoringService
    ) {
        this.service = service;
        this.monitoringService = monitoringService;
    }

    // ========== ENDPOINTS EXISTANTS (avec légère modification) ==========

    @GetMapping
    @Operation(summary = "List all agents with their health status")
    public Collection<AgentView> list() {
        // MODIFICATION : Utilise le monitoring pour enrichir avec le statut
        return monitoringService.getAllAgentsWithStatus();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one agent details")
    public ResponseEntity<AgentView> get(@PathVariable String id) {
        return service.get(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Spawn a new agent")
    public ResponseEntity<AgentView> create(@Valid @RequestBody AgentCreateRequest req) {
        AgentView view = service.spawn(req);
        return ResponseEntity.created(URI.create("/agents/" + view.id())).body(view);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Destroy an agent")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.destroy(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/message")
    @Operation(summary = "Send a message to an agent")
    public ResponseEntity<MessageAck> send(
        @PathVariable String id, 
        @RequestBody MessageRequest req
    ) {
        return ResponseEntity.accepted().body(service.sendMessage(id, req));
    }

    // ========== NOUVEAUX ENDPOINTS MONITORING ==========

    @GetMapping("/active")
    @Operation(summary = "List only active agents (responding to heartbeats)")
    public Collection<AgentView> listActive() {
        return monitoringService.getActiveAgents();
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "List agents by type (CLIENT, VILLE, CAPTEUR)")
    public Collection<AgentView> listByType(@PathVariable String type) {
        return monitoringService.getAgentsByType(type);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get global agent statistics")
    public AgentStatistics getStatistics() {
        return monitoringService.getStatistics();
    }

    @PostMapping("/{id}/heartbeat")
    @Operation(summary = "Record a heartbeat signal from an agent")
    public ResponseEntity<Void> heartbeat(@PathVariable String id) {
        // Vérifie que l'agent existe
        if (service.get(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        monitoringService.recordHeartbeat(id);
        return ResponseEntity.ok().build();
    }
}