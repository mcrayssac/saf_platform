package com.acme.iot.capteur.actor;

import com.acme.iot.city.actors.CapteurActor;
import com.acme.saf.actor.core.ActorContext;
import com.acme.saf.actor.core.RemoteMessageTransport;

import java.util.Map;

/**
 * HttpCapteurActor - Wrapper extending CapteurActor with Kafka-based inter-pod communication.
 * 
 * This actor can send messages to remote actors (in other microservices) via Kafka.
 * The name is kept as HttpCapteurActor for backward compatibility, but it now uses Kafka.
 */
public class HttpCapteurActor extends CapteurActor {
    
    private RemoteMessageTransport kafkaTransport;
    
    public HttpCapteurActor(Map<String, Object> params) {
        super(params);
        // Kafka transport will be injected via setKafkaTransport
    }
    
    /**
     * Set the Kafka transport for inter-pod messaging.
     * This is called by the ActorFactory after actor creation.
     * 
     * @param transport the Kafka message transport
     */
    public void setKafkaTransport(RemoteMessageTransport transport) {
        this.kafkaTransport = transport;
    }
    
    @Override
    public void setContext(ActorContext context) {
        super.setContext(context);
        
        // Inject remote messaging capability using Kafka transport
        if (kafkaTransport != null) {
            context.setRemoteTransport(kafkaTransport);
            System.out.println("HttpCapteurActor: Kafka transport injected into context");
        } else {
            System.out.println("HttpCapteurActor: No Kafka transport available, remote messaging disabled");
        }
    }
    
    @Override
    public void preStart() {
        super.preStart();
        System.out.println("HttpCapteurActor started with Kafka transport support");
    }
}
