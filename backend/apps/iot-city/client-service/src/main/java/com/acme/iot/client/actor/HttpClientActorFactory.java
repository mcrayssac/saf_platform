package com.acme.iot.client.actor;

import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.RemoteMessageTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory for creating HttpClientActor instances in the client-service.
 * 
 * This factory is registered with the ActorSystem and handles creation
 * of ClientActor instances when saf-control sends create-actor commands.
 * 
 * Actors are configured with Kafka transport for inter-pod messaging.
 */
@Slf4j
@Component
public class HttpClientActorFactory implements ActorFactory {
    
    private final RemoteMessageTransport kafkaTransport;
    
    @Autowired
    public HttpClientActorFactory(RemoteMessageTransport kafkaTransport) {
        this.kafkaTransport = kafkaTransport;
        log.info("HttpClientActorFactory initialized with Kafka transport");
    }
    
    @Override
    public Actor create(String actorType, Map<String, Object> params) {
        if ("HttpClientActor".equals(actorType) || "ClientActor".equals(actorType)) {
            HttpClientActor actor = new HttpClientActor(params);
            
            // Inject Kafka transport for remote messaging
            actor.setKafkaTransport(kafkaTransport);
            log.debug("Created HttpClientActor with Kafka transport");
            
            return actor;
        }
        
        throw new IllegalArgumentException("Unknown actor type: " + actorType);
    }
    
    @Override
    public boolean supports(String actorType) {
        return "HttpClientActor".equals(actorType) || "ClientActor".equals(actorType);
    }
}
