package com.acme.saf.actor.core;

/**
 * Represents the lifecycle states of an actor.
 * An actor transitions through these states during its lifetime.
 */
public enum ActorLifecycleState {
    
    /**
     * The actor has been created but not yet started.
     * preStart() has not been called yet.
     */
    CREATED,
    
    /**
     * The actor is starting up.
     * preStart() is being executed.
     */
    STARTING,
    
    /**
     * The actor is running and can process messages.
     * This is the normal operational state.
     */
    RUNNING,
    
    /**
     * The actor is restarting after a failure.
     * preRestart() and postRestart() are being executed.
     */
    RESTARTING,
    
    /**
     * The actor is in the process of stopping.
     * postStop() is being executed.
     */
    STOPPING,
    
    /**
     * The actor has been stopped and can no longer process messages.
     * This is a terminal state.
     */
    STOPPED,
    
    /**
     * The actor has failed and is awaiting supervision decision.
     * The supervisor will decide whether to restart, resume, or stop the actor.
     */
    FAILED
}
