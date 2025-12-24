package com.acme.saf.saf_runtime.controller;

import com.acme.saf.actor.core.*;
import com.acme.saf.actor.core.protocol.ActorCreatedResponse;
import com.acme.saf.actor.core.protocol.CreateActorCommand;
import com.acme.saf.actor.core.protocol.TellActorCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Base controller for actor runtime operations.
 * All microservices should extend this class to provide
 * standard actor creation and message delivery endpoints.
 */
@RestController
@RequestMapping("/runtime")
public abstract class BaseActorRuntimeController {
    
    private static final Logger log = LoggerFactory.getLogger(BaseActorRuntimeController.class);
    
    private final ActorSystem actorSystem;
    private final ActorFactory actorFactory;
    private final String serviceName;
    
    protected BaseActorRuntimeController(ActorSystem actorSystem, ActorFactory actorFactory, String serviceName) {
        this.actorSystem = actorSystem;
        this.actorFactory = actorFactory;
        this.serviceName = serviceName;
    }
    
    @PostMapping("/create-actor")
    public ResponseEntity<ActorCreatedResponse> createActor(@RequestBody CreateActorCommand command) {
        log.info("[{}] Creating actor: type={}, id={}", serviceName, command.getActorType(), command.getActorId());
        
        try {
            // Use ActorSystem spawn with parameters
            ActorRef actorRef = actorSystem.spawn(command.getActorType(), command.getParams());
            
            log.info("[{}] Actor created successfully: {}", serviceName, command.getActorId());
            return ResponseEntity.ok(
                    ActorCreatedResponse.success(
                            command.getActorId(),
                            command.getActorType(),
                            serviceName
                    )
            );
        } catch (Exception e) {
            log.error("[{}] Failed to create actor: {}", serviceName, command.getActorId(), e);
            return ResponseEntity.ok(
                    ActorCreatedResponse.failure(
                            command.getActorId(),
                            e.getMessage()
                    )
            );
        }
    }
    
    @PostMapping("/tell")
    public ResponseEntity<Void> tell(@RequestBody TellActorCommand command) {
        log.info("[{}] Delivering message to actor: {}", serviceName, command.getTargetActorId());
        
        try {
            ActorRef target = actorSystem.getActor(command.getTargetActorId());
            if (target == null) {
                log.error("[{}] Actor not found: {}", serviceName, command.getTargetActorId());
                return ResponseEntity.notFound().build();
            }
            
            ActorRef sender = command.getSenderActorId() != null 
                    ? actorSystem.getActor(command.getSenderActorId()) 
                    : null;
            
            target.tell(command.getMessage(), sender);
            
            log.info("[{}] Message delivered successfully", serviceName);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[{}] Failed to deliver message", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    protected ActorSystem getActorSystem() {
        return actorSystem;
    }
    
    protected ActorFactory getActorFactory() {
        return actorFactory;
    }
    
    protected String getServiceName() {
        return serviceName;
    }
    
    /**
     * List all actors currently running in this service
     */
    @GetMapping("/actors")
    public ResponseEntity<java.util.List<String>> listActors() {
        log.debug("[{}] Listing all actors", serviceName);
        
        try {
            java.util.List<String> actorIds = actorSystem.getAllActorIds();
            log.debug("[{}] Found {} actors", serviceName, actorIds.size());
            return ResponseEntity.ok(actorIds);
        } catch (Exception e) {
            log.error("[{}] Failed to list actors", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get health status of an actor
     */
    @GetMapping("/actors/{id}/health")
    public ResponseEntity<ActorHealthStatus> getActorHealth(@PathVariable String id) {
        log.debug("[{}] Getting health status for actor: {}", serviceName, id);
        
        try {
            ActorHealthStatus health = actorSystem.getActorHealth(id);
            
            if (health.isHealthy()) {
                log.debug("[{}] Actor {} is healthy", serviceName, id);
                return ResponseEntity.ok(health);
            } else {
                log.warn("[{}] Actor {} is unhealthy: {}", serviceName, id, health.getErrorMessage());
                return ResponseEntity.ok(health);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to get health for actor {}", serviceName, id, e);
            return ResponseEntity.internalServerError()
                    .body(ActorHealthStatus.unhealthy(id, null, e.getMessage()));
        }
    }
    
    /**
     * Restart an actor
     */
    @PostMapping("/actors/{id}/restart")
    public ResponseEntity<Void> restartActor(@PathVariable String id) {
        log.info("[{}] Restarting actor: {}", serviceName, id);
        
        try {
            boolean success = actorSystem.restartActor(id);
            
            if (success) {
                log.info("[{}] Actor {} restarted successfully", serviceName, id);
                return ResponseEntity.ok().build();
            } else {
                log.error("[{}] Failed to restart actor {}", serviceName, id);
                return ResponseEntity.internalServerError().build();
            }
        } catch (Exception e) {
            log.error("[{}] Error restarting actor {}", serviceName, id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
