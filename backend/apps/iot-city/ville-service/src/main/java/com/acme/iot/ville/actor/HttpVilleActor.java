package com.acme.iot.ville.actor;

import com.acme.iot.city.actors.VilleActor;
import com.acme.iot.city.messages.AssociateCapteurToVille;
import com.acme.iot.city.messages.RegisterCapteur;
import com.acme.saf.actor.core.ActorContext;
import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.RemoteActorRefProxy;
import com.acme.saf.actor.core.RemoteMessageTransport;
import com.acme.saf.actor.core.SimpleMessage;

import java.util.Map;

/**
 * HttpVilleActor - Wrapper extending VilleActor with Kafka-based inter-pod communication.
 * 
 * This actor can send messages to remote actors (in other microservices) via Kafka.
 * The name is kept as HttpVilleActor for backward compatibility, but it now uses Kafka.
 */
public class HttpVilleActor extends VilleActor {
    
    private RemoteMessageTransport kafkaTransport;
    
    public HttpVilleActor(Map<String, Object> params) {
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
            System.out.println("HttpVilleActor: Kafka transport injected into context");
        } else {
            System.out.println("HttpVilleActor: No Kafka transport available, remote messaging disabled");
        }
    }
    
    @Override
    public void preStart() {
        super.preStart();
        System.out.println("HttpVilleActor started with Kafka transport support");
    }
    
    /**
     * Override to send AssociateCapteurToVille message to the capteur via Kafka.
     * This informs the capteur of its ville association so it can send readings.
     */
    @Override
    protected void onCapteurRegistered(RegisterCapteur req) {
        System.out.println("HttpVilleActor: Sending AssociateCapteurToVille to capteur " + req.getCapteurId());
        
        if (kafkaTransport == null) {
            System.out.println("HttpVilleActor: No Kafka transport, cannot notify capteur");
            return;
        }
        
        // Create the association message
        AssociateCapteurToVille associationMsg = new AssociateCapteurToVille(
            context != null ? context.self().getActorId() : getActorId(),
            getName(),
            getClimateConfig()
        );
        
        // Create a remote reference to the capteur and send the message
        ActorRef capteurRef = new RemoteActorRefProxy(
            req.getCapteurId(), 
            kafkaTransport, 
            context != null ? context.self() : null
        );
        
        try {
            capteurRef.tell(new SimpleMessage(associationMsg), context != null ? context.self() : null);
            System.out.println("HttpVilleActor: AssociateCapteurToVille sent to " + req.getCapteurId());
        } catch (Exception e) {
            System.err.println("HttpVilleActor: Failed to send AssociateCapteurToVille: " + e.getMessage());
        }
    }
}
