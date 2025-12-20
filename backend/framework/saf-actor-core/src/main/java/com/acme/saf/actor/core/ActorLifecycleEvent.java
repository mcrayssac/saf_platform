package com.acme.saf.actor.core;

import java.time.Instant;

/**
 * Base class for actor lifecycle events.
 * These events are emitted during actor lifecycle transitions and can be
 * observed for monitoring, logging, and debugging purposes.
 */
public abstract class ActorLifecycleEvent {
    
    private final String actorId;
    private final String actorPath;
    private final ActorLifecycleState state;
    private final Instant timestamp;
    
    protected ActorLifecycleEvent(String actorId, String actorPath, ActorLifecycleState state) {
        this.actorId = actorId;
        this.actorPath = actorPath;
        this.state = state;
        this.timestamp = Instant.now();
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public String getActorPath() {
        return actorPath;
    }
    
    public ActorLifecycleState getState() {
        return state;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("%s[actorId=%s, path=%s, state=%s, timestamp=%s]",
                getClass().getSimpleName(), actorId, actorPath, state, timestamp);
    }
    
    /**
     * Event emitted when an actor is created.
     */
    public static class ActorCreated extends ActorLifecycleEvent {
        public ActorCreated(String actorId, String actorPath) {
            super(actorId, actorPath, ActorLifecycleState.CREATED);
        }
    }
    
    /**
     * Event emitted when an actor starts (preStart called).
     */
    public static class ActorStarted extends ActorLifecycleEvent {
        public ActorStarted(String actorId, String actorPath) {
            super(actorId, actorPath, ActorLifecycleState.RUNNING);
        }
    }
    
    /**
     * Event emitted when an actor stops (postStop called).
     */
    public static class ActorStopped extends ActorLifecycleEvent {
        private final String reason;
        
        public ActorStopped(String actorId, String actorPath, String reason) {
            super(actorId, actorPath, ActorLifecycleState.STOPPED);
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return String.format("%s[actorId=%s, path=%s, reason=%s]",
                    getClass().getSimpleName(), getActorId(), getActorPath(), reason);
        }
    }
    
    /**
     * Event emitted when an actor fails.
     */
    public static class ActorFailed extends ActorLifecycleEvent {
        private final Throwable cause;
        private final Message failedMessage;
        
        public ActorFailed(String actorId, String actorPath, Throwable cause, Message failedMessage) {
            super(actorId, actorPath, ActorLifecycleState.FAILED);
            this.cause = cause;
            this.failedMessage = failedMessage;
        }
        
        public Throwable getCause() {
            return cause;
        }
        
        public Message getFailedMessage() {
            return failedMessage;
        }
        
        @Override
        public String toString() {
            return String.format("%s[actorId=%s, path=%s, cause=%s]",
                    getClass().getSimpleName(), getActorId(), getActorPath(), 
                    cause != null ? cause.getMessage() : "null");
        }
    }
    
    /**
     * Event emitted when an actor restarts after failure.
     */
    public static class ActorRestarted extends ActorLifecycleEvent {
        private final Throwable cause;
        
        public ActorRestarted(String actorId, String actorPath, Throwable cause) {
            super(actorId, actorPath, ActorLifecycleState.RUNNING);
            this.cause = cause;
        }
        
        public Throwable getCause() {
            return cause;
        }
        
        @Override
        public String toString() {
            return String.format("%s[actorId=%s, path=%s, cause=%s]",
                    getClass().getSimpleName(), getActorId(), getActorPath(),
                    cause != null ? cause.getMessage() : "null");
        }
    }
}
