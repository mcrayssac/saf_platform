package com.acme.iot.capteur.actor;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.RemoteMessageTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating HttpCapteurActor instances in the capteur-service.
 * 
 * This factory is registered with the ActorSystem and handles creation
 * of CapteurActor instances when saf-control sends create-actor commands.
 * 
 * Actors are configured with Kafka transport for inter-pod messaging.
 */
@Slf4j
@Component
public class HttpCapteurActorFactory implements ActorFactory {
    
    private final RemoteMessageTransport kafkaTransport;
    
    @Autowired
    public HttpCapteurActorFactory(RemoteMessageTransport kafkaTransport) {
        this.kafkaTransport = kafkaTransport;
        log.info("HttpCapteurActorFactory initialized with Kafka transport");
    }
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("HttpCapteurActor".equals(actorType) || "CapteurActor".equals(actorType)) {
            HttpCapteurActor actor = new HttpCapteurActor(params);
            
            // Inject Kafka transport for remote messaging
            actor.setKafkaTransport(kafkaTransport);
            log.debug("Created HttpCapteurActor with Kafka transport");
            
            return actor;
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpCapteurActor".equals(actorType) || "CapteurActor".equals(actorType);
    }
}
