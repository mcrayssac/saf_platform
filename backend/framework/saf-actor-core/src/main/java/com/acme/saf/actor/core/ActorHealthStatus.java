package com.acme.saf.actor.core;

/**
 * Represents the health status of an actor
 */
public class ActorHealthStatus {
    
    private String actorId;
    private ActorLifecycleState state;
    private boolean healthy;
    private long lastMessageProcessedAt;
    private int messageQueueSize;
    private String errorMessage;
    
    public ActorHealthStatus() {
    }
    
    public ActorHealthStatus(String actorId, ActorLifecycleState state, boolean healthy,
                            long lastMessageProcessedAt, int messageQueueSize, String errorMessage) {
        this.actorId = actorId;
        this.state = state;
        this.healthy = healthy;
        this.lastMessageProcessedAt = lastMessageProcessedAt;
        this.messageQueueSize = messageQueueSize;
        this.errorMessage = errorMessage;
    }
    
    public static ActorHealthStatus healthy(String actorId, ActorLifecycleState state, 
                                           long lastMessageProcessedAt, int queueSize) {
        return new ActorHealthStatus(actorId, state, true, lastMessageProcessedAt, queueSize, null);
    }
    
    public static ActorHealthStatus unhealthy(String actorId, ActorLifecycleState state, String errorMessage) {
        return new ActorHealthStatus(actorId, state, false, 0, 0, errorMessage);
    }
    
    public static ActorHealthStatus notFound(String actorId) {
        return new ActorHealthStatus(actorId, null, false, 0, 0, "Actor not found");
    }
    
    // Getters and Setters
    public String getActorId() {
        return actorId;
    }
    
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
    
    public ActorLifecycleState getState() {
        return state;
    }
    
    public void setState(ActorLifecycleState state) {
        this.state = state;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
    
    public long getLastMessageProcessedAt() {
        return lastMessageProcessedAt;
    }
    
    public void setLastMessageProcessedAt(long lastMessageProcessedAt) {
        this.lastMessageProcessedAt = lastMessageProcessedAt;
    }
    
    public int getMessageQueueSize() {
        return messageQueueSize;
    }
    
    public void setMessageQueueSize(int messageQueueSize) {
        this.messageQueueSize = messageQueueSize;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
