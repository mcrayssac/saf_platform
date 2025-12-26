package com.acme.iot.city.messages;

import com.acme.saf.actor.core.ActorRef;
import com.acme.saf.actor.core.Message;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Message sent to VilleActor to request detailed city information.
 * The VilleActor will respond with a VilleInfoResponse message.
 */
@JsonTypeName("RequestVilleInfo")
public class RequestVilleInfo implements Message {
    private static final long serialVersionUID = 1L;
    
    private final ActorRef requester;
    
    @JsonCreator
    public RequestVilleInfo(@JsonProperty("requester") ActorRef requester) {
        this.requester = requester;
    }
    
    public ActorRef getRequester() {
        return requester;
    }
    
    @Override
    public Object getPayload() {
        return requester;
    }
    
    @Override
    public String toString() {
        return "RequestVilleInfo{requester=" + (requester != null ? requester.getActorId() : "null") + "}";
    }
}
