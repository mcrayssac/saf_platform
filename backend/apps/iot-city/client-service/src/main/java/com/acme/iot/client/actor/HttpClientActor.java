package com.acme.iot.client.actor;

import com.acme.iot.city.actors.ClientActor;
import com.acme.saf.actor.core.ActorContext;
import com.acme.saf.actor.core.RemoteMessageTransport;

import java.util.Map;

/**
 * HttpClientActor - Wrapper extending ClientActor with Kafka-based inter-pod communication.
 * 
 * This actor can send messages to remote actors (in other microservices) via Kafka.
 * The name is kept as HttpClientActor for backward compatibility, but it now uses Kafka.
 */
public class HttpClientActor extends ClientActor {
    
    private RemoteMessageTransport kafkaTransport;
    
    public HttpClientActor(Map<String, Object> params) {
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
            System.out.println("HttpClientActor: Kafka transport injected into context");
        } else {
            System.out.println("HttpClientActor: No Kafka transport available, remote messaging disabled");
        }
    }
    
    @Override
    public void preStart() {
        super.preStart();
        System.out.println("HttpClientActor started with Kafka transport support");
    }
}
