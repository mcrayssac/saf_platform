package com.acme.iot.city.messages;

import com.acme.saf.actor.core.ActorRef;

/**
 * Internal message - sent from ClientActor to VilleActor when entering a city.
 * VilleActor uses this to register the client for climate report broadcasts.
 */
public class RegisterClient {
    private final ActorRef clientRef;
    
    public RegisterClient(ActorRef clientRef) {
        this.clientRef = clientRef;
    }
    
    public ActorRef getClientRef() {
        return clientRef;
    }
    
    @Override
    public String toString() {
        return "RegisterClient{" +
                "clientRef=" + clientRef.getActorId() +
                '}';
    }
}
