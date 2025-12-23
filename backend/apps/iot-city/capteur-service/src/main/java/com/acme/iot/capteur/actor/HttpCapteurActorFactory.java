package com.acme.iot.capteur.actor;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating HttpCapteurActor instances in the capteur-service.
 * 
 * This factory is registered with the ActorSystem and handles creation
 * of CapteurActor instances when saf-control sends create-actor commands.
 */
@Slf4j
@Component
public class HttpCapteurActorFactory implements ActorFactory {
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("HttpCapteurActor".equals(actorType) || "CapteurActor".equals(actorType)) {
            try {
                return new HttpCapteurActor(params);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create HttpCapteurActor", e);
            }
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpCapteurActor".equals(actorType) || "CapteurActor".equals(actorType);
    }
}
