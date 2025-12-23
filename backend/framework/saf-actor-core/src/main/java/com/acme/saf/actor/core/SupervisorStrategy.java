package com.acme.saf.actor.core;

/**
 * Strategy for supervising child actors when they fail.
 * The supervisor decides what action to take when a child actor throws an exception.
 * 
 * Inspired by Akka's supervision strategies.
 */
public interface SupervisorStrategy {
    
    /**
     * Directive that determines how to handle a failed actor.
     */
    enum Directive {
        /**
         * Restart the actor.
         * The actor's state is lost, but the ActorRef remains valid.
         * preRestart() and postRestart() are called.
         */
        RESTART,
        
        /**
         * Resume the actor without restarting.
         * The actor continues processing the next message.
         * The failed message is discarded.
         */
        RESUME,
        
        /**
         * Stop the actor permanently.
         * The actor is terminated and removed from the system.
         */
        STOP,
        
        /**
         * Escalate the failure to the parent supervisor.
         * The parent's supervisor strategy will handle the failure.
         */
        ESCALATE
    }
    
    /**
     * Decides what action to take when a child actor fails.
     * 
     * @param actorRef the actor that failed
     * @param cause the exception that caused the failure
     * @param message the message being processed when the failure occurred (may be null)
     * @return the directive to apply
     */
    Directive decide(ActorRef actorRef, Throwable cause, Message message);
    
    /**
     * Maximum number of retries within the time window before stopping the actor.
     * -1 means unlimited retries.
     * 
     * @return maximum number of retries
     */
    default int maxRetries() {
        return 10;
    }
    
    /**
     * Time window in milliseconds for counting retries.
     * If an actor fails more than maxRetries() times within this window, it will be stopped.
     * 
     * @return time window in milliseconds
     */
    default long withinTimeRange() {
        return 60000; // 1 minute
    }
    
    /**
     * Called when the maximum number of retries has been exceeded.
     * Default behavior is to stop the actor.
     * 
     * @param actorRef the actor that exceeded retries
     * @return the directive to apply (typically STOP or ESCALATE)
     */
    default Directive onMaxRetriesExceeded(ActorRef actorRef) {
        return Directive.STOP;
    }
}
