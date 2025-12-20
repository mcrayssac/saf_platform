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

    // ========== ENDPOINTS DE BASE ==========

    @GetMapping
    @Operation(summary = "List all agents with their health status")
    public Collection<AgentView> list() {
        return monitoringService.getAllAgentsWithStatus();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one agent details")
    public ResponseEntity<AgentView> get(@PathVariable String id) {
        return service.get(id)
                .map(a -> ResponseEntity.ok(monitoringService.buildAgentView(id)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Spawn a new agent", description = "Creates and starts a new agent with the specified type, location, and supervision policy")
    public ResponseEntity<AgentView> create(@Valid @RequestBody AgentCreateRequest req) {
        AgentView created = service.spawn(req);
        AgentView view = monitoringService.buildAgentView(created.id());
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

    @PostMapping("/{id}/heartbeat")
    @Operation(summary = "Record a heartbeat signal from an agent")
    public ResponseEntity<Void> heartbeat(@PathVariable String id) {
        if (service.get(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        monitoringService.recordHeartbeat(id);
        return ResponseEntity.ok().build();
    }

    // ========== ENDPOINTS DE MONITORING ==========

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

    @GetMapping("/status")
    @Operation(summary = "List all agents with their health status")
    public Collection<AgentView> getAllAgentsWithStatus() {
        return monitoringService.getAllAgentsWithStatus();
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get global agent statistics")
    public AgentStatistics getStatistics() {
        return monitoringService.getStatistics();
    }

    // ========== ENDPOINT DE GESTION DE QUARANTAINE ==========

    /**
     * Libère un agent de la quarantaine.
     */
    @PostMapping("/{id}/release-quarantine")
    @Operation(
            summary = "Release an agent from quarantine",
            description = "Manually releases an agent from quarantine after the underlying issue has been fixed. " +
                    "Use with caution: releasing a still-faulty agent may cause it to fail again."
    )
    public ResponseEntity<Void> releaseFromQuarantine(@PathVariable String id) {
        if (service.get(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        monitoringService.releaseFromQuarantine(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Vérifie si un agent est en quarantaine.
     */
    @GetMapping("/{id}/is-quarantined")
    @Operation(summary = "Check if an agent is currently in quarantine")
    public ResponseEntity<Boolean> isQuarantined(@PathVariable String id) {
        if (service.get(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean quarantined = monitoringService.isInQuarantine(id);
        return ResponseEntity.ok(quarantined);
    }
}