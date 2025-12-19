package com.acme.saf.saf_control.web;

import com.acme.saf.saf_control.application.ControlService;
import com.acme.saf.saf_control.domain.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;

@RestController
@RequestMapping("/agents")
@Tag(name = "Agents")
@SecurityRequirement(name = "X-API-KEY")
public class AgentsController {
    private final ControlService service;

    public AgentsController(ControlService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List agents (mock)")
    public Collection<AgentView> list() { return service.list(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get one agent (mock)")
    public ResponseEntity<AgentView> get(@PathVariable String id) {
        // Vérifier si l'agent existe dans le registre
        AgentView agent = service.get(id);
        
        // Si l'agent est null ou n'existe pas, renvoyer une réponse 404
        if (agent == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Si l'agent existe, renvoyer l'agent avec un code 200 OK
        return ResponseEntity.ok(agent);
    }
    
    @GetMapping("/all")
    @Operation (summary = "Get all agents")
    public ResponseEntity<Collection<AgentView>> getAll() {
        Collection<AgentView> agents = service.getAllAgents();
        return ResponseEntity.ok(agents);
    }
    
    @GetMapping("/host/{host}")
    @Operation(summary = "Get agents by host")
    public ResponseEntity<Collection<AgentView>> getByHost(@PathVariable String host) {
        Collection<AgentView> agents = service.getAgentsByHost(host);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get agents by status")
    public ResponseEntity<Collection<AgentView>> getByStatus(@PathVariable String status) {
        Collection<AgentView> agents = service.getAgentsByStatus(status);
        return ResponseEntity.ok(agents);
    }


    @PostMapping
    @Operation(summary = "Spawn a new agent (mock)")
    public ResponseEntity<AgentView> create(@Valid @RequestBody AgentCreateRequest req) {
        AgentView view = service.create(req);  // Remplacer spawn par create
        return ResponseEntity.created(URI.create("/agents/" + view.id())).body(view);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Destroy an agent (mock)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.destroy(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/message")
    @Operation(summary = "Send a message to an agent (mock)")
    public ResponseEntity<MessageAck> send(@PathVariable String id, @RequestBody MessageRequest req) {
        return ResponseEntity.accepted().body(service.sendMessage(id, req));
    }
}
