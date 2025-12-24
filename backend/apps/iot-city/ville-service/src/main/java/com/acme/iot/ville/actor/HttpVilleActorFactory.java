package com.acme.iot.ville.actor;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating HttpVilleActor instances in the ville-service.
 * 
 * This factory is registered with the ActorSystem and handles creation
 * of VilleActor instances when saf-control sends create-actor commands.
 */
@Slf4j
@Component
public class HttpVilleActorFactory implements ActorFactory {
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("HttpVilleActor".equals(actorType) || "VilleActor".equals(actorType)) {
            return new HttpVilleActor(params);
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpVilleActor".equals(actorType) || "VilleActor".equals(actorType);
    }
}
