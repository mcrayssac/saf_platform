package com.acme.saf.actor.core.protocol;

import java.util.Map;

/**
 * Command to create a new actor in a remote service.
 * Sent from saf-control to microservices.
 */
public class CreateActorCommand {
    
    /**
     * Type of actor to create (e.g., "ClientActor", "VilleActor")
     */
    private String actorType;
    
    /**
     * Unique identifier for the actor
     */
    private String actorId;
    
    /**
     * Parameters for actor initialization
     */
    private Map<String, Object> params;
    
    /**
     * ID of the requesting service/client
     */
    private String requesterId;
    
    public CreateActorCommand() {
    }
    
    public CreateActorCommand(String actorType, String actorId, Map<String, Object> params, String requesterId) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.params = params;
        this.requesterId = requesterId;
    }
    
    public String getActorType() {
        return actorType;
    }
    
    public void setActorType(String actorType) {
        this.actorType = actorType;
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
    
    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
    
    public String getRequesterId() {
        return requesterId;
    }
    
    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
}
