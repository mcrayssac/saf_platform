package com.acme.saf.saf_control.controller;

import com.acme.saf.actor.core.protocol.ActorCreatedResponse;
import com.acme.saf.actor.core.protocol.ActorRegistryEntry;
import com.acme.saf.actor.core.protocol.CreateActorCommand;
import com.acme.saf.actor.core.protocol.TellActorCommand;
import com.acme.saf.saf_control.dto.CreateActorRequest;
import com.acme.saf.saf_control.registry.DistributedActorRegistry;
import com.acme.saf.saf_control.registry.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

/**
 * Controller for distributed actor management.
 * Central routing point for all actor operations across microservices.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/actors")
@RequiredArgsConstructor
public class DistributedActorController {
    
    private final DistributedActorRegistry actorRegistry;
    private final ServiceRegistry serviceRegistry;
    private final WebClient.Builder webClientBuilder;
    
    /**
     * Create a new actor in a distributed microservice
     * POST /api/v1/actors
     */
    @PostMapping
    public ResponseEntity<ActorCreatedResponse> createActor(@RequestBody CreateActorRequest request) {
        log.info("Creating actor: type={}, serviceId={}", request.getActorType(), request.getServiceId());
        
        // 1. Verify service exists
        String serviceUrl = serviceRegistry.getServiceUrl(request.getServiceId())
                .orElseThrow(() -> {
                    log.error("Service not found: {}", request.getServiceId());
                    return new IllegalArgumentException("Service not found: " + request.getServiceId());
                });
        
        // 2. Generate actor ID
        String actorId = UUID.randomUUID().toString();
        
        // 3. Create command
        CreateActorCommand command = new CreateActorCommand(
                request.getActorType(),
                actorId,
                request.getParams(),
                "saf-control"
        );
        
        // 4. Send to microservice
        ActorCreatedResponse response = webClientBuilder.build()
                .post()
                .uri(serviceUrl + "/runtime/create-actor")
                .bodyValue(command)
                .retrieve()
                .bodyToMono(ActorCreatedResponse.class)
                .block();
        
        // 5. Register in distributed registry if successful
        if (response != null && response.isSuccess()) {
            actorRegistry.registerActor(
                    response.getActorId(),
                    response.getActorType(),
                    response.getServiceId(),
                    serviceUrl
            );
            log.info("Actor created and registered: id={}", actorId);
        } else {
            log.error("Failed to create actor: {}", response != null ? response.getErrorMessage() : "No response");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Send a message to an actor (tell pattern)
     * POST /api/v1/actors/{actorId}/tell
     */
    @PostMapping("/{actorId}/tell")
    public ResponseEntity<Void> tellActor(
            @PathVariable String actorId,
            @RequestBody TellActorCommand command) {
        
        log.info("Routing message to actor: {}", actorId);
        
        // 1. Lookup actor in registry
        ActorRegistryEntry entry = actorRegistry.lookupActor(actorId)
                .orElseThrow(() -> {
                    log.error("Actor not found: {}", actorId);
                    return new IllegalArgumentException("Actor not found: " + actorId);
                });
        
        // 2. Route to appropriate service
        webClientBuilder.build()
                .post()
                .uri(entry.getServiceUrl() + "/runtime/tell")
                .bodyValue(command)
                .retrieve()
                .toBodilessEntity()
                .block();
        
        log.info("Message routed successfully to actor: {}", actorId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get all registered actors
     * GET /api/v1/actors
     */
    @GetMapping
    public ResponseEntity<Collection<ActorRegistryEntry>> getAllActors() {
        return ResponseEntity.ok(actorRegistry.getAllActors());
    }
    
    /**
     * Get actor by ID
     * GET /api/v1/actors/{actorId}
     */
    @GetMapping("/{actorId}")
    public ResponseEntity<ActorRegistryEntry> getActor(@PathVariable String actorId) {
        return actorRegistry.lookupActor(actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get actors by service
     * GET /api/v1/actors/by-service/{serviceId}
     */
    @GetMapping("/by-service/{serviceId}")
    public ResponseEntity<Collection<ActorRegistryEntry>> getActorsByService(@PathVariable String serviceId) {
        return ResponseEntity.ok(actorRegistry.getActorsByService(serviceId));
    }
    
    /**
     * Delete an actor
     * DELETE /api/v1/actors/{actorId}
     */
    @DeleteMapping("/{actorId}")
    public ResponseEntity<Void> deleteActor(@PathVariable String actorId) {
        actorRegistry.removeActor(actorId);
        log.info("Actor removed from registry: {}", actorId);
        return ResponseEntity.ok().build();
    }
}
