package com.acme.iot.city.actors;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Factory for creating CapteurActor instances.
 * 
 * Creates sensor actors that publish readings to Kafka topics:
 * "capteur-{sensorType}-{villeId}"
 */
public class CapteurActorFactory implements ActorFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(CapteurActorFactory.class);
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("CapteurActor".equals(actorType)) {
            CapteurActor actor = new CapteurActor(params);
            String sensorType = (String) params.getOrDefault("type", "UNKNOWN");
            String villeId = (String) params.getOrDefault("villeId", "UNKNOWN");
            logger.info("Created CapteurActor: {} for ville: {}", sensorType, villeId);
            return actor;
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "CapteurActor".equals(actorType);
    }
}
