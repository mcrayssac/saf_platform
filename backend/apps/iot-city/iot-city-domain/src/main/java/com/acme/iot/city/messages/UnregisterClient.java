package com.acme.iot.city.messages;

import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

/**
 * Internal message - sent from ClientActor to VilleActor when leaving a city.
 * VilleActor uses this to unregister the client from climate report broadcasts.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class UnregisterClient implements Message {
    private final ActorRef clientRef;
    private String clientId;
    private final String messageType = "UnregisterClient"; // Explicit type marker for deserialization
    
    public UnregisterClient(ActorRef clientRef) {
        this.clientRef = clientRef;
        this.clientId = clientRef != null ? clientRef.getActorId() : null;
    }
    
    // Constructor for string-based messaging (REST API testing)
    public UnregisterClient(String clientId, String villeId) {
        this.clientRef = null;
        this.clientId = clientId;
    }
    
    public ActorRef getClientRef() {
        return clientRef;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getMessageType() {
        return messageType;
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
        return "UnregisterClient{" +
                "clientId=" + clientId +
                '}';
    }
}
