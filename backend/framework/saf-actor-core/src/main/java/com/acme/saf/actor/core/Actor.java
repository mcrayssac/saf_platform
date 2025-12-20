package com.acme.saf.actor.core;

/**
 * Base interface for all actors in the SAF framework.
 * An actor is a computational entity that processes messages sequentially
 * and maintains its own state.
 * 
 * Actors follow these principles:
 * - Process one message at a time (sequential message processing)
 * - Maintain encapsulated state
 * - Communicate only through asynchronous messages
 * - Can create other actors
 * - Can change behavior for the next message
 */
public interface Actor {
    
    /**
     * Called when the actor receives a message.
     * This method should process the message and update internal state as needed.
     * 
     * The actor must handle all expected message types. Unknown messages
     * should be logged and ignored or handled by a default behavior.
     * 
     * @param message the message to process
     * @throws Exception if message processing fails (will trigger supervision)
     */
    void receive(Message message) throws Exception;
    
    /**
     * Called before the actor starts processing messages.
     * Use this for initialization logic (opening connections, loading state, etc.).
     * 
     * @throws Exception if initialization fails
     */
    default void preStart() throws Exception {
        // Default: no-op
    }
    
    /**
     * Called after the actor stops processing messages.
     * Use this for cleanup logic (closing connections, saving state, etc.).
     * 
     * @throws Exception if cleanup fails
     */
    default void postStop() throws Exception {
        // Default: no-op
    }
    
    /**
     * Called when the actor is about to restart after a failure.
     * The actor can perform cleanup before being reinitialized.
     * 
     * @param reason the exception that caused the restart
     * @param message the message being processed when the failure occurred
     * @throws Exception if pre-restart logic fails
     */
    default void preRestart(Throwable reason, Message message) throws Exception {
        postStop();
    }
    
    /**
     * Called after the actor has been restarted.
     * The actor can restore state or reinitialize resources.
     * 
     * @param reason the exception that caused the restart
     * @throws Exception if post-restart logic fails
     */
    default void postRestart(Throwable reason) throws Exception {
        preStart();
    }
}
