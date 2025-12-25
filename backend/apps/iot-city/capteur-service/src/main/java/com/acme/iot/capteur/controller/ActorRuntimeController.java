package com.acme.iot.capteur.controller;

import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.controller.BaseActorRuntimeController;
import com.acme.iot.capteur.actor.HttpCapteurActorFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Actor runtime controller for capteur-service.
 * Extends BaseActorRuntimeController to provide standard actor management endpoints.
 */
@RestController
@RequestMapping("/runtime")
public class ActorRuntimeController extends BaseActorRuntimeController {
    
    public ActorRuntimeController(ActorSystem actorSystem, HttpCapteurActorFactory actorFactory) {
        // WebSocket URL: ws://capteur-service:8083 in Docker, ws://localhost:8083 in dev
        super(actorSystem, actorFactory, "capteur-service", "ws://capteur-service:8083");
    }
}
