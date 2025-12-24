package com.acme.iot.capteur.controller;

import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.controller.BaseActorRuntimeController;
import com.acme.iot.capteur.actor.HttpCapteurActorFactory;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actor runtime controller for capteur-service.
 * Extends BaseActorRuntimeController to provide standard actor management endpoints.
 */
@RestController
public class ActorRuntimeController extends BaseActorRuntimeController {
    
    public ActorRuntimeController(ActorSystem actorSystem, HttpCapteurActorFactory actorFactory) {
        super(actorSystem, actorFactory, "capteur-service");
    }
}
