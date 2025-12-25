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
        // WebSocket URL: ws://ville-service:8082 in Docker, ws://localhost:8082 in dev
        super(actorSystem, actorFactory, "ville-service", "ws://ville-service:8082");
    }
}
