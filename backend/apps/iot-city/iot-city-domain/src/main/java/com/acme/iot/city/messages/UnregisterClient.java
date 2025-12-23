package com.acme.iot.city.messages;

import com.acme.saf.actor.core.ActorRef;

/**
 * Internal message - sent from ClientActor to VilleActor when leaving a city.
 * VilleActor uses this to unregister the client from climate report broadcasts.
 */
public class UnregisterClient {
    private final ActorRef clientRef;
    
    public UnregisterClient(ActorRef clientRef) {
        this.clientRef = clientRef;
    }
    
    public ActorRef getClientRef() {
        return clientRef;
    }
    
    @Override
    public String toString() {
        return "UnregisterClient{" +
                "clientRef=" + clientRef.getActorId() +
                '}';
    }
}
