package com.acme.saf.saf_runtime.controller;

import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.ActorSystem;
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
}
