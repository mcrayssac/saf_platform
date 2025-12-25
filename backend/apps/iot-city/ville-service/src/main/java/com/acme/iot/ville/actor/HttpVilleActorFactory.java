package com.acme.iot.ville.actor;

import com.acme.iot.city.actors.CapteurActorFactory;
import com.acme.iot.ville.controller.WeatherController;
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
 * Also delegates to CapteurActorFactory for creating sensor actors.
 * 
 * Also registers created actors with WeatherController for REST API access.
 */
@Slf4j
@Component
public class HttpVilleActorFactory implements ActorFactory {
    
    private final CapteurActorFactory capteurActorFactory;
    
    public HttpVilleActorFactory(CapteurActorFactory capteurActorFactory) {
        this.capteurActorFactory = capteurActorFactory;
    }
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        // Ensure params is initialized
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        
        // Delegate to CapteurActorFactory if type is CapteurActor
        if ("CapteurActor".equals(actorType)) {
            return capteurActorFactory.create(actorType, params);
        }
        
        if ("HttpVilleActor".equals(actorType) || "VilleActor".equals(actorType)) {
            // Ensure actorId is in params so VilleActor can use it
            String actorId = params.containsKey("actorId") 
                ? params.get("actorId").toString()
                : params.getOrDefault("villeName", "unknown").toString().toLowerCase();
            
            // Add actorId to params if not present
            if (!params.containsKey("actorId")) {
                params.put("actorId", actorId);
            }
            
            HttpVilleActor actor = new HttpVilleActor(params);
            
            // Register with WeatherController for REST API access
            WeatherController.registerVilleActor(actorId, actor);
            log.info("VilleActor registered with WeatherController: {}", actorId);
            
            return actor;
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpVilleActor".equals(actorType) || "VilleActor".equals(actorType) || "CapteurActor".equals(actorType);
    }
}
