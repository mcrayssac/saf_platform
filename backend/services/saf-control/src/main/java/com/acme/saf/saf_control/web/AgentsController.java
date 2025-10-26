package com.acme.saf.saf_control.web;

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

    public AgentsController(ControlService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List agents (mock)")
    public Collection<AgentView> list() { return service.list(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get one agent (mock)")
    public ResponseEntity<AgentView> get(@PathVariable String id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Spawn a new agent (mock)")
    public ResponseEntity<AgentView> create(@Valid @RequestBody AgentCreateRequest req) {
        AgentView view = service.spawn(req);
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
