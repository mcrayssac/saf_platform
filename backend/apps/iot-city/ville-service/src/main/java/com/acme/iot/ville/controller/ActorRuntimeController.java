package com.acme.iot.ville.controller;

import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.controller.BaseActorRuntimeController;
import com.acme.iot.ville.actor.HttpVilleActorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Actor runtime controller for ville-service.
 * Extends BaseActorRuntimeController to provide standard actor management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/runtime")
public class ActorRuntimeController extends BaseActorRuntimeController {
    
    public ActorRuntimeController(ActorSystem actorSystem, HttpVilleActorFactory actorFactory) {
        super(actorSystem, actorFactory, "ville-service");
    }
}
