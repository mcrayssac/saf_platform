package com.acme.saf.actor.core.protocol;

import com.acme.saf.actor.core.ActorLifecycleState;

/**
 * Entry in the distributed actor registry maintained by saf-control.
 * Contains metadata about an actor and its location in the distributed system.
 */
public class ActorRegistryEntry {
    
    private String actorId;
    private String actorType;
    private String serviceId;
    private String serviceUrl;
    private ActorLifecycleState state;
    private long createdAt;
    private long lastUpdated;
    
    public ActorRegistryEntry() {
    }
    
    public ActorRegistryEntry(String actorId, String actorType, String serviceId, String serviceUrl) {
        this.actorId = actorId;
        this.actorType = actorType;
        this.serviceId = serviceId;
        this.serviceUrl = serviceUrl;
        this.state = ActorLifecycleState.CREATED;
        this.createdAt = System.currentTimeMillis();
        this.lastUpdated = this.createdAt;
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
    
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    
    public ActorLifecycleState getState() {
        return state;
    }
    
    public void setState(ActorLifecycleState state) {
        this.state = state;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isActive() {
        return state == ActorLifecycleState.RUNNING || 
               state == ActorLifecycleState.STARTING ||
               state == ActorLifecycleState.BLOCKED;
    }
}
