package com.acme.iot.ville.actor;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.RemoteMessageTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating HttpVilleActor instances in the ville-service.
 * 
 * This factory is registered with the ActorSystem and handles creation
 * of VilleActor instances when saf-control sends create-actor commands.
 * 
 * Actors are configured with Kafka transport for inter-pod messaging.
 */
@Slf4j
@Component
public class HttpVilleActorFactory implements ActorFactory {
    
    private final RemoteMessageTransport kafkaTransport;
    
    @Autowired
    public HttpVilleActorFactory(RemoteMessageTransport kafkaTransport) {
        this.kafkaTransport = kafkaTransport;
        log.info("HttpVilleActorFactory initialized with Kafka transport");
    }
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("HttpVilleActor".equals(actorType) || "VilleActor".equals(actorType)) {
            HttpVilleActor actor = new HttpVilleActor(params);
            
            // Inject Kafka transport for remote messaging
            actor.setKafkaTransport(kafkaTransport);
            log.debug("Created HttpVilleActor with Kafka transport");
            
            return actor;
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpVilleActor".equals(actorType) || "VilleActor".equals(actorType);
    }
}
