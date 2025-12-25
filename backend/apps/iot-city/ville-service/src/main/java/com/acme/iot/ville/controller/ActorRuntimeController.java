package com.acme.iot.ville.controller;

import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.protocol.ActorCreatedResponse;
import com.acme.saf.actor.core.protocol.CreateActorCommand;
import com.acme.saf.saf_runtime.controller.BaseActorRuntimeController;
import com.acme.iot.ville.actor.HttpVilleActorFactory;
import com.acme.iot.city.actors.VilleActor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Actor runtime controller for ville-service.
 * Extends BaseActorRuntimeController to provide standard actor management endpoints.
 * VilleActors are automatically registered with WeatherController by the factory.
 * 
 * When a VilleActor is created, 3 CapteurActors are automatically spawned:
 * - temperature sensor
 * - humidity sensor
 * - pressure sensor
 */
@Slf4j
@RestController
@RequestMapping("/runtime")
public class ActorRuntimeController extends BaseActorRuntimeController {
    
    private final ActorSystem actorSystem;
    
    public ActorRuntimeController(ActorSystem actorSystem, HttpVilleActorFactory actorFactory) {
        super(actorSystem, actorFactory, "ville-service", "ws://ville-service:8083");
        this.actorSystem = actorSystem;
    }
    
    /**
     * Override createActor to use the ID provided by saf-control.
     * Note: Capteurs are created separately on the capteur-service and associated
     * to villes via RegisterCapteur messages.
     */
    @Override
    @PostMapping("/create-actor")
    public ResponseEntity<ActorCreatedResponse> createActor(@RequestBody CreateActorCommand command) {
        log.info("[ville-service] Creating actor: type={}, id={}", command.getActorType(), command.getActorId());
        
        try {
            // Ensure params is initialized
            if (command.getParams() == null) {
                command.setParams(new HashMap<>());
            }
            
            // Always set actorId in params
            command.getParams().put("actorId", command.getActorId());
            
            // Use spawn with the ID provided by saf-control
            // This ensures the distributed registry and local actor system use the same ID
            ActorRef villeRef = actorSystem.spawn(command.getActorType(), command.getActorId(), command.getParams());
            
            log.info("[ville-service] Actor created successfully: {}", command.getActorId());
            return ResponseEntity.ok(
                    ActorCreatedResponse.success(
                            command.getActorId(),
                            command.getActorType(),
                            "ville-service"
                    )
            );
        } catch (Exception e) {
            log.error("[ville-service] Failed to create actor: {}", command.getActorId(), e);
            return ResponseEntity.ok(
                    ActorCreatedResponse.failure(
                            command.getActorId(),
                            e.getMessage()
                    )
            );
        }
    }
}
