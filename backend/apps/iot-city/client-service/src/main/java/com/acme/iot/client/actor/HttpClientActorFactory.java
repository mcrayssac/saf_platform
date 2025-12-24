package com.acme.iot.client.actor;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating HttpClientActor instances in the client-service.
 * 
 * This factory is registered with the ActorSystem and handles creation
 * of ClientActor instances when saf-control sends create-actor commands.
 */
@Slf4j
@Component
public class HttpClientActorFactory implements ActorFactory {
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("HttpClientActor".equals(actorType) || "ClientActor".equals(actorType)) {
            return new HttpClientActor(params);
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpClientActor".equals(actorType) || "ClientActor".equals(actorType);
    }
}
