package com.acme.saf.actor.core.protocol;

import com.acme.saf.actor.core.ActorLifecycleState;

/**
 * Response sent when an actor has been successfully created.
 * Sent from microservices back to saf-control.
 */
public class ActorCreatedResponse {
    
    private String actorId;
    private String actorType;
    private String serviceId;
    private String websocketUrl;  // Direct WebSocket URL for this actor
    private ActorLifecycleState state;
    private String errorMessage;
    
    public ActorCreatedResponse() {
    }
    
    public ActorCreatedResponse(String actorId, String actorType, String serviceId, ActorLifecycleState state) {
        this.actorId = actorId;
        this.actorType = actorType;
        this.serviceId = serviceId;
        this.state = state;
    }
    
    public static ActorCreatedResponse success(String actorId, String actorType, String serviceId) {
        return new ActorCreatedResponse(actorId, actorType, serviceId, ActorLifecycleState.RUNNING);
    }
    
    public static ActorCreatedResponse failure(String actorId, String error) {
        ActorCreatedResponse response = new ActorCreatedResponse();
        response.actorId = actorId;
        response.state = ActorLifecycleState.FAILED;
        response.errorMessage = error;
        return response;
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
    
    public String getActorType() {
        return actorType;
    }
    
    public void setActorType(String actorType) {
        this.actorType = actorType;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getWebsocketUrl() {
        return websocketUrl;
    }
    
    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }
    
    public ActorLifecycleState getState() {
        return state;
    }
    
    public void setState(ActorLifecycleState state) {
        this.state = state;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return state == ActorLifecycleState.RUNNING || state == ActorLifecycleState.STARTING;
    }
}
