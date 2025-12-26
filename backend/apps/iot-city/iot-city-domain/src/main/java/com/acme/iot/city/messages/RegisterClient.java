package com.acme.iot.city.messages;

import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * Internal message - sent from ClientActor to VilleActor when entering a city.
 * VilleActor uses this to register the client for climate report broadcasts.
 * Supports both ActorRef (for inter-actor) and string IDs (for Kafka messaging).
 */
public class RegisterClient implements Message {
    private ActorRef clientRef;
    private String clientId;
    private String villeId;
    
    // Default constructor for Jackson
    public RegisterClient() {
    }
    
    // Constructor for ActorRef-based messaging
    public RegisterClient(ActorRef clientRef) {
        this.clientRef = clientRef;
        this.clientId = clientRef != null ? clientRef.getActorId() : null;
    }
    
    // Constructor for string-based messaging (Kafka)
    public RegisterClient(String clientId, String villeId) {
        this.clientId = clientId;
        this.villeId = villeId;
    }
    
    public ActorRef getClientRef() {
        return clientRef;
    }
    
    public void setClientRef(ActorRef clientRef) {
        this.clientRef = clientRef;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getVilleId() {
        return villeId;
    }
    
    public void setVilleId(String villeId) {
        this.villeId = villeId;
    }
    
    @Override
    @JsonIgnore
    public Object getPayload() {
        return this;
    }
    
    @Override
    @JsonIgnore
    public Instant getTimestamp() {
        return Message.super.getTimestamp();
    }
    
    @Override
    @JsonIgnore
    public String getMessageId() {
        return Message.super.getMessageId();
    }
    
    @Override
    @JsonIgnore
    public String getCorrelationId() {
        return Message.super.getCorrelationId();
    }
    
    @Override
    public String toString() {
        return "RegisterClient{" +
                "clientRef=" + (clientRef != null ? clientRef.getActorId() : "null") +
                ", clientId='" + clientId + '\'' +
                ", villeId='" + villeId + '\'' +
                '}';
    }
}
