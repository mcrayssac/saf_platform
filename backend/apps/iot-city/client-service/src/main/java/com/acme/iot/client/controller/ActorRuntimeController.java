package com.acme.iot.client.controller;

import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.controller.BaseActorRuntimeController;
import com.acme.iot.client.actor.HttpClientActorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actor runtime controller for client-service.
 * Extends BaseActorRuntimeController to provide standard actor management endpoints.
 */
@RestController
public class ActorRuntimeController extends BaseActorRuntimeController {
    
    public ActorRuntimeController(ActorSystem actorSystem, HttpClientActorFactory actorFactory) {
        // WebSocket URL: ws://client-service:8084 in Docker, ws://localhost:8084 in dev
        super(actorSystem, actorFactory, "client-service", "ws://client-service:8084");
    }
}
